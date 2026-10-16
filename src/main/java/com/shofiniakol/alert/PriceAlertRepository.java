package com.shofiniakol.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, UUID> {

    List<PriceAlert> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<PriceAlert> findByUserIdAndOfferId(UUID userId, UUID offerId);

    Optional<PriceAlert> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select a from PriceAlert a
            where a.offerId = :offerId and a.active = true and a.triggeredAt is null and a.targetPrice >= :price
            """)
    List<PriceAlert> findTriggerable(@Param("offerId") UUID offerId, @Param("price") long price);

    @Modifying
    @Query("update PriceAlert a set a.triggeredAt = null where a.offerId = :offerId and a.triggeredAt is not null and a.targetPrice < :price")
    int rearm(@Param("offerId") UUID offerId, @Param("price") long price);

    @Modifying
    @Query("delete from PriceAlert a where a.userId = :userId")
    int deleteByUser(@Param("userId") UUID userId);
}
