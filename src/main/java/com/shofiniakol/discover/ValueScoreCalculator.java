package com.shofiniakol.discover;

import java.util.ArrayList;
import java.util.List;

/**
 * The "Best Value" engine (product brief, section 8).
 *
 * <p>Each offer gets a 0-100 score built from weighted components, all normalised to 0..1:
 * discount, rating, portion density (food per pound versus the category's typical offer),
 * delivery (fee + ETA), distance and budget fit. Components that cannot be computed
 * (e.g. distance when the user shares no location) are dropped and the remaining weights
 * are re-normalised, so scores stay comparable.
 *
 * <p>Pure and dependency-free on purpose: it is unit-tested without Spring.
 */
public final class ValueScoreCalculator {

    /** Discount at or above this percentage earns the full discount score. */
    static final double DISCOUNT_CAP_PERCENT = 50.0;
    /** Bayesian smoothing: an offer needs about this many reviews to outweigh its restaurant's rating. */
    static final double RATING_PRIOR_WEIGHT = 5.0;
    /** Rating assumed for a brand-new restaurant without any review. */
    static final double GLOBAL_PRIOR_RATING = 4.0;

    private ValueScoreCalculator() {
    }

    public record Weights(double discount, double rating, double portion, double delivery,
                          double distance, double budgetFit) {
    }

    public record Input(long price, long originalPrice, double effectiveRating, double portionDensity,
                        double benchmarkDensity, long deliveryFee, int etaMinutes, Double distanceKm, Long budget) {
    }

    public record Component(String key, double score, double weight) {
    }

    public record Result(int score, List<Component> components) {
        public double scoreOf(String key) {
            return components.stream().filter(c -> c.key().equals(key)).mapToDouble(Component::score)
                    .findFirst().orElse(-1);
        }
    }

    public static Result calculate(Input in, Weights w, double maxDistanceKm) {
        List<Component> parts = new ArrayList<>();

        double discountPct = in.originalPrice() > in.price() && in.originalPrice() > 0
                ? (in.originalPrice() - in.price()) * 100.0 / in.originalPrice() : 0;
        parts.add(new Component("discount", clamp(discountPct / DISCOUNT_CAP_PERCENT), w.discount()));

        parts.add(new Component("rating", clamp((in.effectiveRating() - 2.5) / 2.5), w.rating()));

        double portion = in.benchmarkDensity() > 0
                ? clamp(in.portionDensity() / (2 * in.benchmarkDensity())) : 0.5;
        parts.add(new Component("portion", portion, w.portion()));

        double feeScore = in.deliveryFee() <= 0 || in.price() <= 0
                ? 1.0
                : 1.0 - Math.min(in.deliveryFee() / (double) in.price(), 0.4) / 0.4;
        double etaScore = clamp(1.0 - (in.etaMinutes() - 15) / 45.0);
        parts.add(new Component("delivery", 0.7 * feeScore + 0.3 * etaScore, w.delivery()));

        if (in.distanceKm() != null) {
            parts.add(new Component("distance", clamp(1.0 - in.distanceKm() / maxDistanceKm), w.distance()));
        }
        if (in.budget() != null && in.budget() > 0) {
            double ratio = in.price() / (double) in.budget();
            double fit = ratio > 1.0 ? 0.0 : (ratio >= 0.6 ? 1.0 : ratio / 0.6);
            parts.add(new Component("budget_fit", fit, w.budgetFit()));
        }

        double weightSum = 0;
        double weighted = 0;
        for (Component c : parts) {
            weightSum += c.weight();
            weighted += c.weight() * c.score();
        }
        int score = weightSum <= 0 ? 0 : (int) Math.round(100.0 * weighted / weightSum);
        return new Result(Math.max(0, Math.min(100, score)), List.copyOf(parts));
    }

    /**
     * Blends the offer's own reviews with its restaurant's rating so that an offer with one lucky
     * 5-star review does not outrank a proven one.
     */
    public static double effectiveRating(double offerAvg, int offerCount, double restaurantAvg, int restaurantCount) {
        double prior = restaurantCount > 0 ? restaurantAvg : GLOBAL_PRIOR_RATING;
        return (offerCount * offerAvg + RATING_PRIOR_WEIGHT * prior) / (offerCount + RATING_PRIOR_WEIGHT);
    }

    /** Food items per 10,000 pounds. Inflation-proof when compared with the category median. */
    public static double portionDensity(int itemsCount, long price) {
        return price <= 0 ? 0 : itemsCount / (price / 10000.0);
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
