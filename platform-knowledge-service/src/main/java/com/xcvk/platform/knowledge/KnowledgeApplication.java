package com.xcvk.platform.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * knowledge module 启动类
 * port 8083
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-17 15:48
 */
@SpringBootApplication
@EnableDiscoveryClient
public class KnowledgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeApplication.class , args);
    }
}
