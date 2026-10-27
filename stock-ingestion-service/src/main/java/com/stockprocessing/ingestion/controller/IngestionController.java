
package com.stockprocessing.ingestion.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {

    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of(
                "service", "stock-ingestion-service",
                "status", "RUNNING"
        );
    }
}
