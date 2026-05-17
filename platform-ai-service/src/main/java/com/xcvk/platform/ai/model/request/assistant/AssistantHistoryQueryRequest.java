package com.xcvk.platform.ai.model.request.assistant;

/**
 *  获取助手历史记录请求参数。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-18 3:34
 */
public record AssistantHistoryQueryRequest(

        Integer pageNum,

        Integer pageSize

) {

    public int safePageNum() {
        return pageNum == null || pageNum < 1
                ? 1
                : pageNum;
    }

    public int safePageSize() {
        return pageSize == null || pageSize < 1
                ? 10
                : Math.min(pageSize, 50);
    }
}
