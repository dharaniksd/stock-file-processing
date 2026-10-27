package com.stockprocessing.coordinator.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/coordinator")
public class CoordinatorController {

    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of(
                "service", "stock-coordinator-service",
                "status", "RUNNING"
        );
    }
}

