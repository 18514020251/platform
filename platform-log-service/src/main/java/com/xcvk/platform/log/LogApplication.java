package com.xcvk.platform.log;

import com.xcvk.platform.log.starter.properties.AccessLogProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * log module 启动类。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-11
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(AccessLogProperties.class)
public class LogApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogApplication.class, args);
    }
}
