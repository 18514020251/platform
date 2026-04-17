package com.xcvk.platform.log.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Programmer
 * @version 1.0
 * @date 2026-04-17 16:09
 */
@RestController
@RequestMapping("/api/log")
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "log-service ok";
    }
}
