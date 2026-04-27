package com.xcvk.platform.knowledge.search.model.index;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识文档切片搜索索引对象
 *
 * <p>该对象用于承接知识文档切片在 Elasticsearch 中的检索数据模型，
 * 当前阶段先支持 chunk 级全文检索。</p>
 *
 * <p>后续接入向量检索时，可在该索引对象中扩展 dense_vector 字段，
 * 用于存储 chunkText 对应的 embedding 向量。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-26
 */
@Data
@Accessors(chain = true)
@Document(indexName = "kb_chunk", writeTypeHint = WriteTypeHint.FALSE)
public class KnowledgeChunkIndex implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 切片ID
     *
     * <p>作为 ES 文档主键，通常与 kb_document_chunk.id 保持一致，
     * 便于后续更新、删除和重建索引。</p>
     */
    @Id
    private Long id;

    /**
     * 知识文档ID
     */
    @Field(type = FieldType.Long)
    private Long documentId;

    /**
     * 切片序号
     */
    @Field(type = FieldType.Integer)
    private Integer chunkNo;

    /**
     * 切片文本
     *
     * <p>当前阶段用于 chunk 级全文检索；
     * 后续也会作为生成 embedding 的原始文本。</p>
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String chunkText;

    /**
     * 切片内容哈希
     *
     * <p>用于标识 chunk 内容指纹，便于后续判断是否需要重新向量化。</p>
     */
    @Field(type = FieldType.Keyword)
    private String chunkHash;

    /**
     * token 数量
     *
     * <p>当前阶段可先用字符数近似，后续接入 tokenizer 后再替换为真实 token 统计。</p>
     */
    @Field(type = FieldType.Integer)
    private Integer tokenCount;

    /**
     * 切片状态
     *
     * <p>如 ACTIVE / OFFLINE，用于过滤已下线内容。</p>
     */
    @Field(type = FieldType.Keyword)
    private String status;

    /**
     * 文档标题快照
     *
     * <p>冗余文档标题，方便 RAG 返回引用来源时展示。</p>
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String documentTitle;

    /**
     * 分类ID
     */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /**
     * 分类名称快照
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String categoryName;

    /**
     * 标签
     */
    @Field(type = FieldType.Keyword)
    private String tags;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updatedAt;
}