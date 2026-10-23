package com.shofiniakol.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByRestaurantIdOrderBySortOrderAscNameAsc(UUID restaurantId);

    List<MenuItem> findByRestaurantIdAndAvailableTrueOrderBySortOrderAscNameAsc(UUID restaurantId);

    Optional<MenuItem> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    long countByRestaurantIdInAndAvailableTrue(Collection<UUID> restaurantIds);
}
