
package com.stockprocessing.monitoring.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonitoringControllerTest {

    private final MonitoringController controller =
            new MonitoringController();

    @Test
    void shouldReturnRunningStatus() {

        Map<String, String> response =
                controller.status();

        assertEquals(
                "stock-monitoring-service",
                response.get("service")
        );

        assertEquals(
                "RUNNING",
                response.get("status")
        );
    }
}
