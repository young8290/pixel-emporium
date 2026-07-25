package com.example.business.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/")
    public String hello() {
        return "hello business module";
    }

    @GetMapping("hi")
    public String hi() {
        return "hi business module";
    }
}
