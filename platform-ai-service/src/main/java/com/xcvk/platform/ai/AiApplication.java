package com.xcvk.platform.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ai module 启动类
 * port:8084
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-17 14:59
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class , args);
    }
}
