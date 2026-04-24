package com.xcvk.platform.knowledge.search.constant;

/**
 * 知识文档搜索字段常量
 *
 * <p>用于统一收口 KnowledgeDocumentIndex 在 Elasticsearch 中使用到的字段名、
 * 高亮标签和搜索策略相关常量，避免业务代码中散落大量魔法字符串。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-24 10:26
 */
public final class KnowledgeSearchFields {

    private KnowledgeSearchFields() {
    }

    public static final String ID = "id";

    public static final String DOCUMENT_ID = "documentId";

    public static final String TITLE = "title";

    public static final String SUMMARY = "summary";

    public static final String CONTENT = "content";

    public static final String CATEGORY_ID = "categoryId";

    public static final String CATEGORY_NAME = "categoryName";

    public static final String TAGS = "tags";

    public static final String STATUS = "status";

    public static final String CREATOR_ID = "creatorId";

    public static final String CREATOR_NAME = "creatorName";

    public static final String CREATED_AT = "createdAt";

    public static final String UPDATED_AT = "updatedAt";

    public static final String PUBLISHED_AT = "publishedAt";

    public static final String HIGHLIGHT_TITLE = TITLE;

    public static final String HIGHLIGHT_PRE_TAG = "<em>";

    public static final String HIGHLIGHT_POST_TAG = "</em>";

    public static final String MINIMUM_SHOULD_MATCH_ONE = "1";
}