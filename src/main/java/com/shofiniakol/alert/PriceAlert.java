package com.shofiniakol.alert;

import com.shofiniakol.common.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "price_alerts")
public class PriceAlert extends AuditedEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(name = "target_price", nullable = false)
    private long targetPrice;

    @Column(nullable = false)
    private boolean active = true;

    /** Set when the alert fired; cleared again if the price climbs back above the target. */
    @Column(name = "triggered_at")
    private Instant triggeredAt;
}
