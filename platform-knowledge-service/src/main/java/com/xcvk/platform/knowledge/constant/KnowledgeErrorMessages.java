package com.xcvk.platform.knowledge.constant;

/**
 * 知识库错误提示常量
 *
 * <p>用于统一收口知识库模块中的业务错误提示，避免 Service 中散落魔法字符串。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-24
 */
public final class KnowledgeErrorMessages {

    private KnowledgeErrorMessages() {
    }

    public static final String DOCUMENT_ID_REQUIRED = "知识文档ID不能为空";

    public static final String UPDATE_DOCUMENT_REQUEST_REQUIRED = "更新知识文档请求不能为空";

    public static final String CURRENT_LOGIN_IDENTITY_REQUIRED = "当前登录身份不能为空";

    public static final String CURRENT_USER_ID_REQUIRED = "当前登录用户ID不能为空";

    public static final String CURRENT_USERNAME_REQUIRED = "当前登录用户名不能为空";

    public static final String CREATE_DOCUMENT_REQUEST_REQUIRED = "创建知识文档请求不能为空";

    public static final String CREATE_DOCUMENT_CMD_REQUIRED = "创建知识文档命令不能为空";

    public static final String TITLE_REQUIRED = "知识标题不能为空";

    public static final String CONTENT_REQUIRED = "知识正文不能为空";

    public static final String CREATOR_ID_REQUIRED = "创建人ID不能为空";

    public static final String CREATOR_NAME_REQUIRED = "创建人名称不能为空";

    public static final String CREATE_DOCUMENT_FAILED = "创建知识文档失败";

    public static final String QUERY_REQUIRED = "查询条件不能为空";

    public static final String UPDATE_DOCUMENT_FAILED =  "更新知识文档失败";

    public static final String DOCUMENT_NOT_FOUND = "知识文档不存在";

    public static final String OFFLINE_DOCUMENT_FAILED = "下线知识文档失败";

    public static final String DOCUMENT_REQUIRED = "知识文档不能为空";

    public static final String SAVE_DOCUMENT_CHUNK_FAILED = "保存知识文档切片失败";

    public static final String DOCUMENT_CHUNK_NOT_FOUND = "知识文档切片不存在";

    public static final String DOCUMENT_CHUNK_TEXT_REQUIRED = "知识文档切片文本不能为空";

    public static final String EMBEDDING_RESPONSE_INVALID = "文本向量化响应无效";

    public static final String EMBEDDING_MODEL_REQUIRED = "向量模型名称不能为空";

    public static final String EMBEDDING_DIMENSION_REQUIRED = "向量维度不能为空";

    public static final String EMBEDDING_DIMENSION_INVALID = "向量维度不合法";

    public static final String EMBEDDING_VECTOR_REQUIRED = "向量结果不能为空";

    public static final String EMBEDDING_VECTOR_COUNT_NOT_MATCH = "向量数量与切片数量不一致";

    public static final String EMBEDDING_VECTOR_DIMENSION_NOT_MATCH = "向量维度与模型维度不一致";

}