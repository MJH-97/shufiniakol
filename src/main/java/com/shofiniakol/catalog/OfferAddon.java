package com.shofiniakol.catalog;

import com.shofiniakol.common.IdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Optional extra the customer may pick (the app's "choose 2 extras"). extra_price 0 = free choice. */
@Getter
@Setter
@Entity
@Table(name = "offer_addons")
public class OfferAddon extends IdEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "extra_price", nullable = false)
    private long extraPrice;

    @Column(nullable = false)
    private boolean available = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
