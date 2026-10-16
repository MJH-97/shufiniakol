package com.shofiniakol.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    List<PriceHistory> findByOfferIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID offerId, Instant since);
}
