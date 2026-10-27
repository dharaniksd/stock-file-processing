
package com.stockprocessing.monitoring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    @GetMapping("/status")
    public Map<String, String> status() {

        return Map.of(
                "service", "stock-monitoring-service",
                "status", "RUNNING"
        );
    }
}
