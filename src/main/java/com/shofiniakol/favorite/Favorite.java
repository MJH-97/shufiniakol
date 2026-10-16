package com.shofiniakol.favorite;

import com.shofiniakol.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** A saved offer OR a saved restaurant (exactly one of the two ids is set). */
@Getter
@Setter
@Entity
@Table(name = "favorites")
public class Favorite extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "offer_id")
    private UUID offerId;

    @Column(name = "restaurant_id")
    private UUID restaurantId;
}
