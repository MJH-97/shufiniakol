package com.shofiniakol.order;

import com.shofiniakol.common.IdEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Price snapshot of one cart line, so later offer edits never change past orders. */
@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItem extends IdEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "offer_id")
    private UUID offerId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(name = "original_unit_price", nullable = false)
    private long originalUnitPrice;

    /** Sum of the paid add-ons for ONE unit. */
    @Column(name = "addons_total", nullable = false)
    private long addonsTotal;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false)
    private long lineTotal;

    @Column(length = 300)
    private String note;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemAddon> addons = new ArrayList<>();
}
