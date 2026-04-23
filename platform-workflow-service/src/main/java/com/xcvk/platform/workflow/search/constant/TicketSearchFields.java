package com.xcvk.platform.workflow.search.constant;

/**
 * 工单搜索字段常量
 *
 * <p>用于统一收口 TicketIndex 在 Elasticsearch 中使用到的字段名、
 * 高亮标签和搜索策略相关常量，避免业务代码中散落大量魔法字符串。</p>
 *
 * <p>当前阶段主要用于：</p>
 * <ul>
 *     <li>查询字段名统一管理</li>
 *     <li>排序字段名统一管理</li>
 *     <li>高亮字段和标签统一管理</li>
 *     <li>搜索布尔查询策略值统一管理</li>
 * </ul>
 *
 * @author Programmer
 * @since 2026-04-23
 */
public final class TicketSearchFields {

    private TicketSearchFields() {
    }

    /**
     * 工单编号
     */
    public static final String TICKET_NO = "ticketNo";

    /**
     * 工单标题
     */
    public static final String TITLE = "title";

    /**
     * 工单正文
     */
    public static final String CONTENT = "content";

    /**
     * 当前状态
     */
    public static final String STATUS = "status";

    /**
     * 工单类型编码
     */
    public static final String TICKET_TYPE_CODE = "ticketTypeCode";

    /**
     * 工单来源
     */
    public static final String SOURCE = "source";

    /**
     * 创建人 ID
     */
    public static final String CREATOR_ID = "creatorId";

    /**
     * 处理人 ID
     */
    public static final String ASSIGNEE_ID = "assigneeId";

    /**
     * 更新时间
     */
    public static final String UPDATED_AT = "updatedAt";

    /**
     * 创建时间
     */
    public static final String CREATED_AT = "createdAt";

    /**
     * 标题高亮字段
     */
    public static final String HIGHLIGHT_TITLE = TITLE;

    /**
     * 高亮前缀标签
     */
    public static final String HIGHLIGHT_PRE_TAG = "<em>";

    /**
     * 高亮后缀标签
     */
    public static final String HIGHLIGHT_POST_TAG = "</em>";

    /**
     * should 条件至少命中 1 个
     */
    public static final String MINIMUM_SHOULD_MATCH_ONE = "1";
}