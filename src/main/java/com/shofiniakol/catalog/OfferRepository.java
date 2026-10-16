package com.shofiniakol.catalog;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID>, JpaSpecificationExecutor<Offer> {

    @Modifying
    @Query("update Offer o set o.viewCount = o.viewCount + 1 where o.id = :id")
    void incrementViews(@Param("id") UUID id);

    /**
     * Atomically takes {@code qty} units. Unlimited offers (stock null) always succeed because
     * NULL - qty stays NULL. Returns 0 when there is not enough stock.
     */
    @Modifying
    @Query("""
            update Offer o set o.stockRemaining = o.stockRemaining - :qty, o.orderCount = o.orderCount + :qty
            where o.id = :id and (o.stockRemaining is null or o.stockRemaining >= :qty)
            """)
    int reserveStock(@Param("id") UUID id, @Param("qty") int qty);

    @Modifying
    @Query("""
            update Offer o set o.stockRemaining = o.stockRemaining + :qty, o.orderCount = o.orderCount - :qty
            where o.id = :id
            """)
    void releaseStock(@Param("id") UUID id, @Param("qty") int qty);

    @Modifying
    @Query("update Offer o set o.ratingAvg = :avg, o.ratingCount = :cnt where o.id = :id")
    void updateRating(@Param("id") UUID id, @Param("avg") double avg, @Param("cnt") int cnt);

    @Query("""
            select o from Offer o
            where o.active = true and o.expiryNotified = false
              and o.endsAt is not null and o.endsAt > :now and o.endsAt <= :until
            """)
    List<Offer> findExpiringSoon(@Param("now") Instant now, @Param("until") Instant until);

    /** Projection used for per-category portion benchmarks: [categoryId, itemsCount, price]. */
    @Query("select o.category.id, o.itemsCount, o.price from Offer o where o.active = true and o.price > 0")
    List<Object[]> findBenchmarkRows();

    long countByRestaurantId(UUID restaurantId);

    @Query("select o from Offer o where o.restaurant.id in :restaurantIds")
    List<Offer> findTopByRestaurants(@Param("restaurantIds") Collection<UUID> restaurantIds, Pageable pageable);
}
