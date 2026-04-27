package com.xcvk.platform.knowledge.search.constant;

/**
 * 知识文档切片搜索字段常量
 *
 * <p>用于统一收口 KnowledgeChunkIndex 在 Elasticsearch 中使用到的字段名、
 * 高亮标签和搜索策略相关常量，避免业务代码中散落魔法字符串。</p>
 *
 * <p>该常量类服务于 chunk 级检索，主要用于后续 AI / RAG 检索相关知识片段。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-27
 */
public final class KnowledgeChunkSearchFields {

    private KnowledgeChunkSearchFields() {
    }

    /**
     * 切片ID
     */
    public static final String ID = "id";

    /**
     * 知识文档ID
     */
    public static final String DOCUMENT_ID = "documentId";

    /**
     * 切片序号
     */
    public static final String CHUNK_NO = "chunkNo";

    /**
     * 切片文本
     */
    public static final String CHUNK_TEXT = "chunkText";

    /**
     * 切片内容哈希
     */
    public static final String CHUNK_HASH = "chunkHash";

    /**
     * token 数量
     */
    public static final String TOKEN_COUNT = "tokenCount";

    /**
     * 切片状态
     */
    public static final String STATUS = "status";

    /**
     * 文档标题
     */
    public static final String DOCUMENT_TITLE = "documentTitle";

    /**
     * 分类ID
     */
    public static final String CATEGORY_ID = "categoryId";

    /**
     * 分类名称
     */
    public static final String CATEGORY_NAME = "categoryName";

    /**
     * 标签
     */
    public static final String TAGS = "tags";

    /**
     * 创建时间
     */
    public static final String CREATED_AT = "createdAt";

    /**
     * 更新时间
     */
    public static final String UPDATED_AT = "updatedAt";

    /**
     * 切片文本高亮字段
     */
    public static final String HIGHLIGHT_CHUNK_TEXT = CHUNK_TEXT;

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