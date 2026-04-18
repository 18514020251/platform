package com.xcvk.platform.id.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ID 生成配置
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18
 */
@Data
@ConfigurationProperties(prefix = "platform.id")
public class IdProperties {

    /**
     * 工作节点ID
     */
    private long workerId = 1;

    /**
     * 数据中心ID
     */
    private long datacenterId = 1;
}