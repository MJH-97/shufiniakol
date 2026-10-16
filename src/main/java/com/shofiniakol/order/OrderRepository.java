package com.shofiniakol.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndStatusIn(UUID userId, Collection<OrderStatus> statuses);

    Optional<Order> findByUserIdAndClientRequestId(UUID userId, String clientRequestId);

    Page<Order> findByUserIdAndStatusIn(UUID userId, Collection<OrderStatus> statuses, Pageable pageable);

    @Query(value = "select nextval('order_number_seq')", nativeQuery = true)
    long nextOrderSequence();

    // ---- dashboard statistics: [count, revenue, commission, customer savings] of DELIVERED orders ----
    @Query("""
            select count(o), coalesce(sum(o.total), 0), coalesce(sum(o.commissionAmount), 0), coalesce(sum(o.savingTotal), 0)
            from CustomerOrder o where o.status = :status and o.createdAt >= :from and o.createdAt < :to
            """)
    List<Object[]> statsAll(@Param("status") OrderStatus status, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select count(o), coalesce(sum(o.total), 0), coalesce(sum(o.commissionAmount), 0), coalesce(sum(o.savingTotal), 0)
            from CustomerOrder o where o.status = :status and o.createdAt >= :from and o.createdAt < :to
              and o.restaurant.id in :restaurantIds
            """)
    List<Object[]> statsForRestaurants(@Param("status") OrderStatus status, @Param("from") Instant from,
                                       @Param("to") Instant to, @Param("restaurantIds") Collection<UUID> restaurantIds);

    @Query("select o.status, count(o) from CustomerOrder o where o.createdAt >= :from and o.createdAt < :to group by o.status")
    List<Object[]> countByStatusAll(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select o.status, count(o) from CustomerOrder o
            where o.createdAt >= :from and o.createdAt < :to and o.restaurant.id in :restaurantIds group by o.status
            """)
    List<Object[]> countByStatusForRestaurants(@Param("from") Instant from, @Param("to") Instant to,
                                               @Param("restaurantIds") Collection<UUID> restaurantIds);
}
