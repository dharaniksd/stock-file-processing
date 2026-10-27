
package com.stockprocessing.ingestion.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IngestionControllerTest {

    private final IngestionController controller =
            new IngestionController();

    @Test
    void shouldReturnRunningStatus() {
        var response = controller.status();

        assertEquals(
                "stock-ingestion-service",
                response.get("service")
        );

        assertEquals(
                "RUNNING",
                response.get("status")
        );
    }
}
