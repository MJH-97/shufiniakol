package com.shofiniakol.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, UUID> {

    List<DeliveryZone> findByRestaurantIdOrderByMaxDistanceKmAsc(UUID restaurantId);

    Optional<DeliveryZone> findByIdAndRestaurantId(UUID id, UUID restaurantId);
}
