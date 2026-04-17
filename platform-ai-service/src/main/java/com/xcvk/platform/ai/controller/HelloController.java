package com.xcvk.platform.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Programmer
 * @version 1.0
 * @date 2026-04-17 15:57
 */
@RestController
@RequestMapping("/api/ai")
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "ai-service ok";
    }
}
