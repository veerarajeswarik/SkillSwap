package com.example.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health/status check: GET http://localhost:8080/ shows that the backend is up.
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "SkillSwap Backend is Running!";
    }
}
