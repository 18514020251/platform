package com.xcvk.platform.workflow.search.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.xcvk.platform.auth.starter.constant.PlatformRoleConstants;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.workflow.assembler.TicketAssembler;
import com.xcvk.platform.workflow.constant.TicketErrorMessages;
import com.xcvk.platform.workflow.model.query.TicketManageQuery;
import com.xcvk.platform.workflow.model.vo.TicketManageListItemVO;
import com.xcvk.platform.workflow.search.model.index.TicketIndex;
import com.xcvk.platform.workflow.search.service.TicketSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.*;

/**
 * 工单搜索服务实现类
 *
 * <p>当前阶段基于 Elasticsearch 提供处理侧工单搜索能力，
 * 实现最小可用的搜索闭环：</p>
 * <ul>
 *     <li>关键词搜索：ticketNo、title</li>
 *     <li>条件过滤：status、ticketTypeCode、source、creatorId、assigneeId</li>
 *     <li>权限范围控制：管理员 / 支持人员</li>
 *     <li>分页与排序</li>
 * </ul>
 *
 * <p>当前版本先不引入高亮、聚合、拼音搜索等增强能力，
 * 优先保证检索链路清晰、易懂、可验证。</p>
 *
 * @author Programmer
 * @since 2026-04-23
 */
@Service
@RequiredArgsConstructor
public class TicketSearchServiceImpl implements TicketSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final TicketAssembler ticketAssembler;

    /**
     * 处理侧工单搜索。
     *
     * @param identity 当前登录身份
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<TicketManageListItemVO> searchManageTickets(CurrentLoginIdentity identity, TicketManageQuery query) {
        validateCurrentLoginIdentity(identity);
        BizAssert.notNull(query, ErrorCode.PARAM_INVALID, TicketErrorMessages.QUERY_REQUIRED);

        int pageNum = query.safePageNum();
        int pageSize = query.safePageSize();

        var boolQuery = bool();

        applyManagePermissionScope(boolQuery, identity, query);

        applyManageQueryFilters(boolQuery, query);

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(boolQuery.build()._toQuery())
                .withPageable(PageRequest.of(pageNum - 1, pageSize))
                .withSort(Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("createdAt")))
                .build();

        SearchHits<TicketIndex> searchHits = elasticsearchOperations.search(searchQuery, TicketIndex.class);

        List<TicketManageListItemVO> records = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(ticketAssembler::toTicketManageListItemVO)
                .toList();

        long total = searchHits.getTotalHits();

        return PageResult.of(records, total, pageNum, pageSize);
    }

    /**
     * 应用处理侧数据权限范围。
     *
     * <p>与 MySQL 版处理侧列表保持一致：</p>
     * <ul>
     *     <li>管理员：可查看全部</li>
     *     <li>支持人员 + mineOnly=true：仅查看分配给自己的工单</li>
     *     <li>支持人员 + unassignedOnly=true：仅查看未分派工单</li>
     *     <li>支持人员默认：查看“分配给自己”或“未分派”的工单</li>
     * </ul>
     *
     * @param boolQuery bool 查询构造器
     * @param identity 当前登录身份
     * @param query 查询条件
     */
    private void applyManagePermissionScope(BoolQuery.Builder boolQuery,
                                            CurrentLoginIdentity identity,
                                            TicketManageQuery query) {
        Long currentUserId = identity.userId();
        List<String> roleCodes = identity.roleCodes();

        boolean isAdmin = hasRole(roleCodes, PlatformRoleConstants.ADMIN);
        boolean isSupport = hasRole(roleCodes, PlatformRoleConstants.SUPPORT);

        BizAssert.isTrue(
                isAdmin || isSupport,
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.MANAGE_PERMISSION_DENIED
        );

        if (isAdmin) {
            return;
        }

        if (query.mineOnlyOrFalse()) {
            boolQuery.filter(term(t -> t.field("assigneeId").value(currentUserId)));
            return;
        }

        if (query.unassignedOnlyOrFalse()) {
            boolQuery.mustNot(term(t -> t.field("assigneeId").value(currentUserId)));
            boolQuery.mustNot(term(t -> t.field("assigneeId").value(0L)));
            return;
        }

        boolQuery.must(q -> q.bool(b -> b
                .should(term(t -> t.field("assigneeId").value(currentUserId)))
                .should(q2 -> q2.bool(b2 -> b2.mustNot(mn -> mn.exists(e -> e.field("assigneeId")))))
                .minimumShouldMatch("1")
        ));
    }

    /**
     * 应用处理侧查询条件。
     *
     * @param boolQuery bool 查询构造器
     * @param query 查询条件
     */
    private void applyManageQueryFilters(BoolQuery.Builder boolQuery,
                                         TicketManageQuery query) {
        if (StringUtils.hasText(query.keyword())) {
            String keyword = query.keyword().trim();

            boolQuery.must(q -> q.bool(b -> b
                    .should(term(t -> t.field("ticketNo").value(keyword)))
                    .should(match(m -> m.field("title").query(keyword)))
                    .minimumShouldMatch("1")
            ));
        }

        if (StringUtils.hasText(query.status())) {
            boolQuery.filter(term(t -> t.field("status").value(query.status().trim())));
        }

        if (StringUtils.hasText(query.ticketTypeCode())) {
            boolQuery.filter(term(t -> t.field("ticketTypeCode").value(query.ticketTypeCode().trim())));
        }

        if (StringUtils.hasText(query.source())) {
            boolQuery.filter(term(t -> t.field("source").value(query.source().trim())));
        }

        if (query.creatorId() != null) {
            boolQuery.filter(term(t -> t.field("creatorId").value(query.creatorId())));
        }

        if (query.assigneeId() != null) {
            boolQuery.filter(term(t -> t.field("assigneeId").value(query.assigneeId())));
        }
    }

    /**
     * 判断当前角色列表中是否包含指定角色。
     *
     * @param roleCodes 角色编码列表
     * @param targetRole 目标角色
     * @return true 表示包含
     */
    private boolean hasRole(List<String> roleCodes, String targetRole) {
        if (CollectionUtils.isEmpty(roleCodes) || !StringUtils.hasText(targetRole)) {
            return false;
        }
        return roleCodes.contains(targetRole);
    }

    /**
     * 校验当前登录身份。
     *
     * @param identity 当前登录身份
     */
    private void validateCurrentLoginIdentity(CurrentLoginIdentity identity) {
        BizAssert.notNull(identity, ErrorCode.PARAM_INVALID, TicketErrorMessages.CURRENT_LOGIN_IDENTITY_REQUIRED);
    }
}