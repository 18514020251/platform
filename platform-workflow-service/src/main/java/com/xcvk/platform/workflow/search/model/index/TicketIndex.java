package com.xcvk.platform.workflow.search.model.index;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单搜索索引对象
 *
 * <p>该对象用于承接工单在 Elasticsearch 中的检索数据模型，
 * 与 MySQL 中的 Ticket 实体解耦，避免搜索字段设计反向污染数据库实体。</p>
 *
 * <p>当前阶段优先支撑工单全文搜索场景：
 * 标题、内容、工单编号、工单类型、创建人、处理人、状态、来源、优先级等。</p>
 *
 * <p>后续如需支持高亮、聚合统计、拼音搜索、语义检索等能力，
 * 可继续在该索引对象上扩展，而不影响主库表结构。</p>
 *
 * @author Programmer
 * @since 2026-04-23
 */
@Data
@Accessors(chain = true)
@Document(indexName = "wf_ticket", writeTypeHint = WriteTypeHint.FALSE)
public class TicketIndex implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工单ID
     *
     * <p>作为 ES 文档主键，通常与业务主键保持一致，
     * 便于根据工单ID做增量更新、删除和数据同步。</p>
     */
    @Id
    private Long id;

    /**
     * 工单编号
     *
     * <p>适合精确匹配、筛选和展示，不做分词。</p>
     */
    @Field(type = FieldType.Keyword)
    private String ticketNo;

    /**
     * 工单类型ID
     */
    @Field(type = FieldType.Long)
    private Long ticketTypeId;

    /**
     * 工单类型编码
     *
     * <p>用于精确过滤，比如按某个工单类型筛选。</p>
     */
    @Field(type = FieldType.Keyword)
    private String ticketTypeCode;

    /**
     * 工单类型名称
     *
     * <p>支持按中文名称搜索，因此使用 text，并指定 IK 分词器。</p>
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String ticketTypeName;

    /**
     * 工单标题
     *
     * <p>标题是最核心的全文检索字段，适合细粒度分词建立倒排索引。</p>
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /**
     * 工单内容
     *
     * <p>正文搜索字段，适合全文检索。</p>
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    /**
     * 当前状态
     *
     * <p>如 PENDING / PROCESSING / RESOLVED / REJECTED，
     * 用于精确过滤，不做分词。</p>
     */
    @Field(type = FieldType.Keyword)
    private String status;

    /**
     * 优先级
     *
     * <p>如 HIGH / MEDIUM / LOW，用于过滤和排序展示。</p>
     */
    @Field(type = FieldType.Keyword)
    private String priority;

    /**
     * 工单来源
     *
     * <p>如 MANUAL / AI_AGENT，用于过滤。</p>
     */
    @Field(type = FieldType.Keyword)
    private String source;

    /**
     * 来源关联ID
     *
     * <p>一般用于精确定位外部来源对象，不做分词。</p>
     */
    @Field(type = FieldType.Keyword)
    private String sourceRef;

    /**
     * 创建人ID
     */
    @Field(type = FieldType.Long)
    private Long creatorId;

    /**
     * 创建人名称快照
     *
     * <p>支持按创建人姓名搜索。</p>
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String creatorName;

    /**
     * 当前处理人ID
     */
    @Field(type = FieldType.Long)
    private Long assigneeId;

    /**
     * 当前处理人名称快照
     *
     * <p>支持按处理人姓名搜索。</p>
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String assigneeName;

    /**
     * 当前状态说明/处理备注
     *
     * <p>用于补充搜索上下文，例如拒绝原因、处理说明等。</p>
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String statusRemark;

    /**
     * 关闭时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime closedAt;

    /**
     * 创建时间
     *
     * <p>用于排序和时间范围过滤。</p>
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * <p>用于处理侧列表排序。</p>
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updatedAt;
}