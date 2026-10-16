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

import java.util.UUID;

/** One line of "what is in this meal" (e.g. 2 x chicken shawarma). */
@Getter
@Setter
@Entity
@Table(name = "offer_items")
public class OfferItem extends IdEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @Column(name = "menu_item_id")
    private UUID menuItemId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
