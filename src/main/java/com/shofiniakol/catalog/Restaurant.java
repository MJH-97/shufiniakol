package com.shofiniakol.catalog;

import com.shofiniakol.common.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "restaurants")
public class Restaurant extends AuditedEntity {

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 80)
    private String city = "دمشق";

    @Column(length = 80)
    private String area;

    @Column(name = "address_line", length = 250)
    private String addressLine;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    @Column(name = "accepting_orders", nullable = false)
    private boolean acceptingOrders = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "default_delivery_fee", nullable = false)
    private long defaultDeliveryFee;

    @Column(name = "min_order_amount", nullable = false)
    private long minOrderAmount;

    @Column(name = "avg_prep_minutes", nullable = false)
    private int avgPrepMinutes = 25;

    @Column(name = "commission_percent", nullable = false)
    private double commissionPercent = 10;

    @Column(name = "rating_avg", nullable = false)
    private double ratingAvg;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @ManyToMany
    @JoinTable(name = "restaurant_categories",
            joinColumns = @JoinColumn(name = "restaurant_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "restaurant")
    private List<DeliveryZone> deliveryZones = new ArrayList<>();

    /** Opening hours may wrap past midnight (e.g. 12:00 - 02:00). Missing hours mean "always open". */
    public boolean isOpenAt(LocalTime t) {
        if (opensAt == null || closesAt == null) {
            return true;
        }
        if (opensAt.equals(closesAt)) {
            return true;
        }
        if (opensAt.isBefore(closesAt)) {
            return !t.isBefore(opensAt) && t.isBefore(closesAt);
        }
        return !t.isBefore(opensAt) || t.isBefore(closesAt);
    }
}
