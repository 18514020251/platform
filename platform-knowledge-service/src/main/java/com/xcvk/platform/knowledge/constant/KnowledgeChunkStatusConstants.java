package com.xcvk.platform.knowledge.constant;

/**
 * 知识文档切片状态常量
 *
 * <p>chunk 是由知识文档派生出来的检索数据，
 * 当前阶段只需要区分可用和下线两种状态。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-26
 */
public final class KnowledgeChunkStatusConstants {

    private KnowledgeChunkStatusConstants() {
    }

    /**
     * 可用
     */
    public static final String ACTIVE = "ACTIVE";

    /**
     * 已下线
     */
    public static final String OFFLINE = "OFFLINE";
}