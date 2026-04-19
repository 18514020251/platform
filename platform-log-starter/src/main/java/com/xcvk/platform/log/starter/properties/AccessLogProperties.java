package com.xcvk.platform.log.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 访问日志配置
 *
 * <p>用于控制访问日志是否启用、默认是否记录参数/返回值、
 * 敏感字段脱敏和日志内容最大长度等基础行为。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-19
 */
@ConfigurationProperties(prefix = "platform.access-log")
public class AccessLogProperties {

    /**
     * 是否启用访问日志
     */
    private boolean enabled = true;

    /**
     * 默认是否记录请求参数
     */
    private boolean recordArgs = true;

    /**
     * 默认是否记录响应结果
     */
    private boolean recordResult = false;

    /**
     * 单段日志内容最大长度，超过后截断
     */
    private int maxContentLength = 2000;

    /**
     * 需要脱敏的字段名
     */
    private List<String> sensitiveFields = new ArrayList<>(List.of(
            "password",
            "token",
            "accessToken",
            "refreshToken",
            "authorization",
            "secret",
            "credential"
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRecordArgs() {
        return recordArgs;
    }

    public void setRecordArgs(boolean recordArgs) {
        this.recordArgs = recordArgs;
    }

    public boolean isRecordResult() {
        return recordResult;
    }

    public void setRecordResult(boolean recordResult) {
        this.recordResult = recordResult;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public List<String> getSensitiveFields() {
        return sensitiveFields;
    }

    public void setSensitiveFields(List<String> sensitiveFields) {
        this.sensitiveFields = sensitiveFields;
    }
}