
package com.stockprocessing.worker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/worker")
public class WorkerController {

    @GetMapping("/status")
    public Map<String, String> status() {

        return Map.of(
                "service", "stock-worker-service",
                "status", "RUNNING"
        );
    }
}
