package com.shofiniakol.discover;

import com.shofiniakol.catalog.DeliveryPricingService.DeliveryQuote;
import com.shofiniakol.catalog.Offer;

/** An offer together with everything computed for one particular user/query. */
public record ScoredOffer(Offer offer, Double distanceKm, DeliveryQuote quote, double effectiveRating,
                          ValueScoreCalculator.Result value, boolean restaurantOpen, boolean limitedTime,
                          boolean available) {
}
