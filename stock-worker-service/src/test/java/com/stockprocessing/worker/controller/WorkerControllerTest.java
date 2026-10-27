package com.stockprocessing.worker.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkerControllerTest {
    private final WorkerController controller =
            new WorkerController();

    @Test
    void shouldReturnRunningStatus() {

        var response = controller.status();

        assertEquals(
                "stock-worker-service",
                response.get("service")
        );

        assertEquals(
                "RUNNING",
                response.get("status")
        );
    }
}
