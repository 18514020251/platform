package com.xcvk.platform.workflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * workflow module 启动类
 * port 8082
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-17 15:52
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.xcvk.platform.workflow.repository.mapper")
public class WorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class ,  args);
    }
}
