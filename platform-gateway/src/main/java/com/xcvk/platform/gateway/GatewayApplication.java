package com.xcvk.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * gateway module 启动类
 * port  8080
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-17 16:03
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {
    // TODO 全局判断接口
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class , args);
    }
}
