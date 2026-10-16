package com.shofiniakol.catalog;

import com.shofiniakol.common.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Distance-tier delivery pricing: the smallest tier whose max_distance_km covers the customer wins. */
@Getter
@Setter
@Entity
@Table(name = "delivery_zones")
public class DeliveryZone extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "max_distance_km", nullable = false)
    private double maxDistanceKm;

    @Column(nullable = false)
    private long fee;

    @Column(name = "min_order_amount", nullable = false)
    private long minOrderAmount;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes = 30;

    @Column(nullable = false)
    private boolean active = true;
}
