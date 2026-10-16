package com.shofiniakol.discover;

import com.shofiniakol.discover.ValueScoreCalculator.Input;
import com.shofiniakol.discover.ValueScoreCalculator.Result;
import com.shofiniakol.discover.ValueScoreCalculator.Weights;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueScoreCalculatorTest {

    private static final Weights W = new Weights(0.25, 0.25, 0.20, 0.10, 0.10, 0.10);
    private static final long BUDGET = 50_000;
    // typical offer in the category: 0.6 food items per 10,000 pounds
    private static final double BENCHMARK = 0.6;

    private static Result score(long price, long original, double rating, int items, long fee, int eta, Double km) {
        Input in = new Input(price, original, rating, ValueScoreCalculator.portionDensity(items, price),
                BENCHMARK, fee, eta, km, BUDGET);
        return ValueScoreCalculator.calculate(in, W, 10);
    }

    /** Product brief section 8: meal A (45K, 4.6*, 25% off, full meal) beats meal B (48K, 4.1*, 5%, less food). */
    @Test
    void briefExample_fullDiscountedMealBeatsCheaperLookingOne() {
        Result a = score(45_000, 60_000, 4.6, 4, 5_000, 30, 1.2);
        Result b = score(48_000, 50_526, 4.1, 2, 5_000, 30, 1.2);
        assertTrue(a.score() > b.score(), "A=" + a.score() + " must beat B=" + b.score());
        assertTrue(a.score() >= 70, "A should be a high-value pick but was " + a.score());
    }

    @Test
    void cheapestIsNotAutomaticallyBest() {
        Result cheapButPoor = score(25_000, 26_000, 3.0, 1, 5_000, 45, 1.2);
        Result goodValue = score(45_000, 60_000, 4.6, 4, 5_000, 30, 1.2);
        assertTrue(goodValue.score() > cheapButPoor.score());
    }

    @Test
    void freeDeliveryImprovesDeliveryComponent() {
        Result paid = score(40_000, 50_000, 4.5, 3, 8_000, 30, 2.0);
        Result free = score(40_000, 50_000, 4.5, 3, 0, 30, 2.0);
        assertTrue(free.scoreOf("delivery") > paid.scoreOf("delivery"));
        assertTrue(free.score() >= paid.score());
    }

    @Test
    void missingDistanceAndBudgetDropTheirComponents() {
        Input in = new Input(40_000, 50_000, 4.5, 0.75, BENCHMARK, 0, 30, null, null);
        Result r = ValueScoreCalculator.calculate(in, W, 10);
        assertEquals(-1.0, r.scoreOf("distance"));
        assertEquals(-1.0, r.scoreOf("budget_fit"));
        assertEquals(4, r.components().size());
        assertTrue(r.score() > 0 && r.score() <= 100);
    }

    @Test
    void scoreIsAlwaysBetween0And100() {
        Result worst = score(500_000, 500_000, 1.0, 1, 200_000, 300, 500.0);
        Result best = score(10_000, 100_000, 5.0, 20, 0, 10, 0.0);
        assertTrue(worst.score() >= 0 && worst.score() < 30, "worst=" + worst.score());
        assertTrue(best.score() <= 100 && best.score() > 85, "best=" + best.score());
    }

    @Test
    void effectiveRatingIsBayesianSmoothed() {
        // one single 5-star review on a restaurant rated 3.0 should stay close to 3.0
        double eff = ValueScoreCalculator.effectiveRating(5.0, 1, 3.0, 100);
        assertTrue(eff < 3.5, "eff=" + eff);
        // many reviews dominate the prior
        double proven = ValueScoreCalculator.effectiveRating(4.8, 200, 3.0, 100);
        assertTrue(proven > 4.7, "proven=" + proven);
        // no reviews anywhere -> global prior
        assertEquals(ValueScoreCalculator.GLOBAL_PRIOR_RATING,
                ValueScoreCalculator.effectiveRating(0, 0, 0, 0), 1e-9);
    }

    @Test
    void portionDensityHandlesZeroPrice() {
        assertEquals(0.0, ValueScoreCalculator.portionDensity(3, 0));
        assertEquals(0.75, ValueScoreCalculator.portionDensity(3, 40_000), 1e-9);
    }
}
