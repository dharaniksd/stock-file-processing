
package com.stockprocessing.coordinator.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_records")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "record_number", nullable = false)
    private Long recordNumber;

    @Column(name = "stock_symbol", nullable = false, length = 32)
    private String stockSymbol;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(
            name = "price",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal price;

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdAt;

    public StockRecord(
            Long jobId,
            Long recordNumber,
            String stockSymbol,
            Long quantity,
            BigDecimal price
    ) {
        this.jobId = jobId;
        this.recordNumber = recordNumber;
        this.stockSymbol = stockSymbol;
        this.quantity = quantity;
        this.price = price;
    }
}
