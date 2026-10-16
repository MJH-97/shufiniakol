package com.shofiniakol.user;

import com.shofiniakol.common.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "addresses")
public class Address extends AuditedEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AddressType type = AddressType.HOME;

    @Column(length = 60)
    private String label;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 80)
    private String area;

    @Column(length = 160)
    private String street;

    @Column(length = 40)
    private String building;

    @Column(length = 20)
    private String floor;

    @Column(length = 20)
    private String apartment;

    @Column(length = 200)
    private String landmark;

    private Double latitude;

    private Double longitude;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    /** One-line human readable form, used as the order's address snapshot. */
    public String toDisplayText() {
        StringBuilder sb = new StringBuilder();
        append(sb, city);
        append(sb, area);
        append(sb, street);
        if (building != null && !building.isBlank()) {
            append(sb, "بناء " + building);
        }
        if (floor != null && !floor.isBlank()) {
            append(sb, "طابق " + floor);
        }
        if (apartment != null && !apartment.isBlank()) {
            append(sb, "شقة " + apartment);
        }
        append(sb, landmark);
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("، ");
        }
        sb.append(part.trim());
    }
}
