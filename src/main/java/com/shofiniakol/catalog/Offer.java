package com.shofiniakol.catalog;

import com.shofiniakol.common.AuditedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A sellable meal deal. The heart of the product: ranked by budget fit and "best value". */
@Getter
@Setter
@Entity
@Table(name = "offers")
public class Offer extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Current price after discount. */
    @Column(nullable = false)
    private long price;

    @Column(name = "original_price", nullable = false)
    private long originalPrice;

    /** Total number of food items inside (sum of item quantities); feeds the portion score. */
    @Column(name = "items_count", nullable = false)
    private int itemsCount = 1;

    @Column(nullable = false)
    private int servings = 1;

    /** How many add-ons the customer may pick per unit (0 = none). */
    @Column(name = "max_addons", nullable = false)
    private int maxAddons;

    /** null = unlimited. */
    @Column(name = "stock_remaining")
    private Integer stockRemaining;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(nullable = false)
    private boolean active = true;

    /** Paid promotion. Shown transparently to users; never changes the value score. */
    @Column(nullable = false)
    private boolean sponsored;

    @Column(name = "rating_avg", nullable = false)
    private double ratingAvg;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "order_count", nullable = false)
    private long orderCount;

    @Column(name = "expiry_notified", nullable = false)
    private boolean expiryNotified;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "offer_images", joinColumns = @JoinColumn(name = "offer_id"))
    @OrderColumn(name = "sort_index")
    @Column(name = "url", nullable = false, length = 500)
    private List<String> images = new ArrayList<>();

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<OfferItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<OfferAddon> addons = new ArrayList<>();

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OfferAvailability> availability = new ArrayList<>();
}
