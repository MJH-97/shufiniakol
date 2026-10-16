package com.shofiniakol.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByOrderIdAndOfferIdIsNull(UUID orderId);

    Page<Review> findByRestaurantIdAndOfferIdIsNullAndHiddenFalseOrderByCreatedAtDesc(UUID restaurantId, Pageable p);

    Page<Review> findByOfferIdAndHiddenFalseOrderByCreatedAtDesc(UUID offerId, Pageable p);

    Page<Review> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId, Pageable p);

    /** [avg, count] of visible restaurant-level reviews. */
    @Query("select avg(r.rating), count(r) from Review r where r.restaurantId = :id and r.offerId is null and r.hidden = false")
    List<Object[]> restaurantStats(@Param("id") UUID restaurantId);

    /** [avg, count] of visible meal-level reviews. */
    @Query("select avg(r.rating), count(r) from Review r where r.offerId = :id and r.hidden = false")
    List<Object[]> offerStats(@Param("id") UUID offerId);
}
