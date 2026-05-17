package com.xcvk.platform.api.contract.knowledge.model;

/**
 * 检索模式
 * */
public enum RetrievalMode {
    // 关键词
    KEYWORD,
    // 向量
    VECTOR,
    // 混合
    HYBRID_RRF,
    // 增强(重写)
    ENHANCED_RRF
}