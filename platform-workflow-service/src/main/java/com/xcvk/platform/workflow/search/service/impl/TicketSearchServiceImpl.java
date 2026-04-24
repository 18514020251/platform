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
import com.xcvk.platform.workflow.search.constant.TicketSearchFields;
import com.xcvk.platform.workflow.search.model.index.TicketIndex;
import com.xcvk.platform.workflow.search.service.TicketSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
 * <p>当前版本先不引入聚合、拼音搜索等增强能力，
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

        BoolQuery.Builder boolQuery = bool();
        applyManagePermissionScope(boolQuery, identity, query);
        applyManageQueryFilters(boolQuery, query);

        NativeQuery searchQuery = buildSearchQuery(boolQuery, pageNum, pageSize);
        SearchHits<TicketIndex> searchHits = elasticsearchOperations.search(searchQuery, TicketIndex.class);

        return buildSearchPageResult(searchHits, pageNum, pageSize);
    }

    /**
     * 构建工单搜索查询对象。
     *
     * <p>统一收口分页、排序和高亮配置，
     * 避免主流程方法中混入过多 Elasticsearch 查询细节。</p>
     *
     * @param boolQuery bool 查询条件
     * @param pageNum 页码，从 1 开始
     * @param pageSize 分页大小
     * @return Elasticsearch 原生查询对象
     */
    private NativeQuery buildSearchQuery(BoolQuery.Builder boolQuery, int pageNum, int pageSize) {
        return NativeQuery.builder()
                .withQuery(boolQuery.build()._toQuery())
                .withPageable(PageRequest.of(pageNum - 1, pageSize))
                .withSort(buildSort())
                .withHighlightQuery(buildTitleHighlightQuery())
                .build();
    }

    /**
     * 构建工单搜索排序规则。
     *
     * <p>当前阶段处理侧列表优先按更新时间倒序，
     * 更新时间相同则按创建时间倒序。</p>
     *
     * @return 排序规则
     */
    private Sort buildSort() {
        return Sort.by(
                Sort.Order.desc(TicketSearchFields.UPDATED_AT),
                Sort.Order.desc(TicketSearchFields.CREATED_AT)
        );
    }

    /**
     * 将 Elasticsearch 搜索结果转换为分页返回对象。
     *
     * <p>如果搜索结果中存在标题高亮，则优先使用高亮标题；
     * 否则回退原始标题。</p>
     *
     * @param searchHits Elasticsearch 搜索结果
     * @param pageNum 当前页码
     * @param pageSize 当前分页大小
     * @return 处理侧工单分页结果
     */
    private PageResult<TicketManageListItemVO> buildSearchPageResult(SearchHits<TicketIndex> searchHits,
                                                                     int pageNum,
                                                                     int pageSize) {
        List<TicketManageListItemVO> records = searchHits.getSearchHits().stream()
                .map(searchHit -> {
                    TicketIndex ticketIndex = searchHit.getContent();
                    String highlightTitle = extractHighlightTitle(searchHit);
                    return ticketAssembler.toTicketManageListItemVO(ticketIndex, highlightTitle);
                })
                .toList();

        long total = searchHits.getTotalHits();
        return PageResult.of(records, total, pageNum, pageSize);
    }

    /**
     * 构建标题高亮查询配置。
     *
     * <p>当前阶段仅对 title 字段做高亮展示，
     * 命中部分统一使用 em 标签包裹。</p>
     *
     * @return 高亮查询配置
     */
    private HighlightQuery buildTitleHighlightQuery() {
        return new HighlightQuery(
                new Highlight(
                        HighlightParameters.builder()
                                .withPreTags(TicketSearchFields.HIGHLIGHT_PRE_TAG)
                                .withPostTags(TicketSearchFields.HIGHLIGHT_POST_TAG)
                                .build(),
                        List.of(new HighlightField(TicketSearchFields.HIGHLIGHT_TITLE))
                ),
                TicketIndex.class
        );
    }

    /**
     * 应用处理侧数据权限范围。
     *
     * <p>与 MySQL 版处理侧列表保持一致：</p>
     * <ul>
     *     <li>管理员：可查看全部</li>
     *     <li>支持人员 + mineOnly=true：仅查看分配给自己的工单</li>
     *     <li>支持人员 + unassignedOnly=true：仅查看未分派工单</li>
     *     <li>支持人员默认：查看"分配给自己"或"未分派"的工单</li>
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
            boolQuery.filter(term(t -> t.field(TicketSearchFields.ASSIGNEE_ID).value(currentUserId)));
            return;
        }

        if (query.unassignedOnlyOrFalse()) {
            boolQuery.mustNot(q -> q.exists(e -> e.field(TicketSearchFields.ASSIGNEE_ID)));
            return;
        }

        boolQuery.must(q -> q.bool(b -> b
                .should(term(t -> t.field(TicketSearchFields.ASSIGNEE_ID).value(currentUserId)))
                .should(q2 -> q2.bool(b2 -> b2.mustNot(mn -> mn.exists(e -> e.field(TicketSearchFields.ASSIGNEE_ID)))))
                .minimumShouldMatch(TicketSearchFields.MINIMUM_SHOULD_MATCH_ONE)
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
                    .should(term(t -> t.field(TicketSearchFields.TICKET_NO).value(keyword)))
                    .should(match(m -> m.field(TicketSearchFields.TITLE).query(keyword)))
                    .should(match(m -> m.field(TicketSearchFields.CONTENT).query(keyword)))
                    .minimumShouldMatch(TicketSearchFields.MINIMUM_SHOULD_MATCH_ONE)
            ));
        }

        if (StringUtils.hasText(query.status())) {
            boolQuery.filter(term(t -> t.field(TicketSearchFields.STATUS).value(query.status().trim())));
        }

        if (StringUtils.hasText(query.ticketTypeCode())) {
            boolQuery.filter(term(t -> t.field(TicketSearchFields.TICKET_TYPE_CODE).value(query.ticketTypeCode().trim())));
        }

        if (StringUtils.hasText(query.source())) {
            boolQuery.filter(term(t -> t.field(TicketSearchFields.SOURCE).value(query.source().trim())));
        }

        if (query.creatorId() != null) {
            boolQuery.filter(term(t -> t.field(TicketSearchFields.CREATOR_ID).value(query.creatorId())));
        }

        if (query.assigneeId() != null) {
            boolQuery.filter(term(t -> t.field(TicketSearchFields.ASSIGNEE_ID).value(query.assigneeId())));
        }
    }

    /**
     * 提取标题高亮内容。
     *
     * <p>如果当前搜索结果命中了 title 字段并返回高亮片段，
     * 则优先使用高亮后的标题；否则返回 null，由装配层回退到原始标题。</p>
     *
     * @param searchHit ES 搜索结果项
     * @return 高亮标题；若无高亮则返回 null
     */
    private String extractHighlightTitle(SearchHit<TicketIndex> searchHit) {
        if (searchHit == null) {
            return null;
        }

        Map<String, List<String>> highlightFields = searchHit.getHighlightFields();
        if (highlightFields.isEmpty()) {
            return null;
        }

        List<String> titleHighlights = highlightFields.getOrDefault(
                TicketSearchFields.HIGHLIGHT_TITLE,
                Collections.emptyList()
        );
        if (titleHighlights.isEmpty()) {
            return null;
        }

        return titleHighlights.get(0);
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
