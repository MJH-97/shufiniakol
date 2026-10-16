package com.shofiniakol.discover;

import com.shofiniakol.catalog.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Median "food items per 10,000 pounds" of the active offers in each category. The portion score is
 * judged against this benchmark, so it stays stable across filters and survives price inflation.
 * Cached for a few minutes; call {@link #invalidate()} after catalog changes.
 */
@Service
@RequiredArgsConstructor
public class CategoryBenchmarkService {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final OfferRepository offerRepository;

    private volatile Map<UUID, Double> byCategory = Map.of();
    private volatile double globalMedian = 0;
    private volatile Instant loadedAt = Instant.EPOCH;

    public double medianDensity(UUID categoryId) {
        refreshIfStale();
        return byCategory.getOrDefault(categoryId, globalMedian);
    }

    public void invalidate() {
        loadedAt = Instant.EPOCH;
    }

    private synchronized void refreshIfStale() {
        if (Duration.between(loadedAt, Instant.now()).compareTo(TTL) < 0) {
            return;
        }
        Map<UUID, List<Double>> groups = new HashMap<>();
        List<Double> all = new ArrayList<>();
        for (Object[] row : offerRepository.findBenchmarkRows()) {
            UUID category = (UUID) row[0];
            int items = ((Number) row[1]).intValue();
            long price = ((Number) row[2]).longValue();
            double density = ValueScoreCalculator.portionDensity(items, price);
            groups.computeIfAbsent(category, k -> new ArrayList<>()).add(density);
            all.add(density);
        }
        Map<UUID, Double> medians = new HashMap<>();
        groups.forEach((k, v) -> medians.put(k, median(v)));
        this.byCategory = medians;
        this.globalMedian = median(all);
        this.loadedAt = Instant.now();
    }

    private static double median(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compare);
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }
}
