package com.shofiniakol.catalog;

import com.shofiniakol.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "price_history")
public class PriceHistory extends BaseEntity {

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(nullable = false)
    private long price;

    @Column(name = "original_price", nullable = false)
    private long originalPrice;
}
