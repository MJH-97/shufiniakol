package com.shofiniakol.discover;

import com.shofiniakol.catalog.AvailabilityService;
import com.shofiniakol.catalog.DeliveryPricingService;
import com.shofiniakol.catalog.DeliveryPricingService.DeliveryQuote;
import com.shofiniakol.catalog.Offer;
import com.shofiniakol.catalog.OfferRepository;
import com.shofiniakol.catalog.Restaurant;
import com.shofiniakol.common.GeoUtils;
import com.shofiniakol.common.Money;
import com.shofiniakol.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Finds, filters, scores and sorts offers. Shared by discover, offers list, home and nearby. */
@Service
@RequiredArgsConstructor
public class OfferRankingService {

    private final OfferRepository offerRepository;
    private final AppProperties props;
    private final CategoryBenchmarkService benchmarks;
    private final DeliveryPricingService delivery;
    private final AvailabilityService availability;

    public record Query(Long budget, boolean budgetIncludesDelivery, UUID categoryId, UUID restaurantId,
                        Double lat, Double lng, Double maxDistanceKm, Double minRating, Long minPrice,
                        boolean openNowOnly, boolean limitedOnly) {
    }

    @Transactional(readOnly = true)
    public List<ScoredOffer> find(Query q, SortOption sort) {
        Instant now = Instant.now();
        Instant endsBefore = q.limitedOnly() ? now.plus(Duration.ofHours(props.getDiscover().getLimitedTimeHours())) : null;
        boolean hasLocation = GeoUtils.validLatLng(q.lat(), q.lng());

        List<Offer> offers = offerRepository.findAll(
                OfferSpecs.servable(now, q.budget(), q.minPrice(), q.categoryId(), q.restaurantId(), endsBefore));

        List<ScoredOffer> out = new ArrayList<>();
        for (Offer o : offers) {
            if (!availability.isOfferAvailable(o, now)) {
                continue;
            }
            Restaurant r = o.getRestaurant();
            boolean open = availability.isRestaurantOpen(r, now);
            if (q.openNowOnly() && !open) {
                continue;
            }
            Double distance = hasLocation ? GeoUtils.distanceKm(q.lat(), q.lng(), r.getLatitude(), r.getLongitude()) : null;
            if (q.maxDistanceKm() != null && distance != null && distance > q.maxDistanceKm()) {
                continue;
            }
            ScoredOffer s = score(o, q.budget(), distance, now, open);
            if (q.minRating() != null && s.effectiveRating() < q.minRating()) {
                continue;
            }
            if (q.budget() != null && q.budgetIncludesDelivery()
                    && o.getPrice() + s.quote().fee() > q.budget()) {
                continue;
            }
            out.add(s);
        }
        out.sort(comparator(sort));
        return out;
    }

    /** Scores a single offer (detail page, favourites) without applying any filter. */
    @Transactional(readOnly = true)
    public ScoredOffer scoreOne(Offer o, Long budget, Double lat, Double lng) {
        Instant now = Instant.now();
        Double distance = GeoUtils.validLatLng(lat, lng)
                ? GeoUtils.distanceKm(lat, lng, o.getRestaurant().getLatitude(), o.getRestaurant().getLongitude())
                : null;
        return score(o, budget, distance, now, availability.isRestaurantOpen(o.getRestaurant(), now));
    }

    /** Cheapest servable offer, used to suggest a higher budget when nothing fits. */
    @Transactional(readOnly = true)
    public Long cheapestPrice(UUID categoryId) {
        Instant now = Instant.now();
        return offerRepository.findAll(OfferSpecs.servable(now, null, null, categoryId, null, null)).stream()
                .filter(o -> availability.isOfferAvailable(o, now))
                .map(Offer::getPrice).min(Long::compare).orElse(null);
    }

    private ScoredOffer score(Offer o, Long budget, Double distance, Instant now, boolean open) {
        Restaurant r = o.getRestaurant();
        DeliveryQuote quote = delivery.quote(r, distance);
        double eff = ValueScoreCalculator.effectiveRating(o.getRatingAvg(), o.getRatingCount(),
                r.getRatingAvg(), r.getRatingCount());
        AppProperties.Weights w = props.getDiscover().getWeights();
        var weights = new ValueScoreCalculator.Weights(w.getDiscount(), w.getRating(), w.getPortion(),
                w.getDelivery(), w.getDistance(), w.getBudgetFit());
        var input = new ValueScoreCalculator.Input(o.getPrice(), o.getOriginalPrice(), eff,
                ValueScoreCalculator.portionDensity(o.getItemsCount(), o.getPrice()),
                benchmarks.medianDensity(o.getCategory().getId()), quote.fee(), quote.etaMinutes(),
                distance, budget);
        var result = ValueScoreCalculator.calculate(input, weights, props.getDiscover().getMaxDistanceKm());
        boolean limited = o.getEndsAt() != null
                && o.getEndsAt().isBefore(now.plus(Duration.ofHours(props.getDiscover().getLimitedTimeHours())));
        return new ScoredOffer(o, distance, quote, eff, result, open, limited, availability.isOfferAvailable(o, now));
    }

    private static Comparator<ScoredOffer> comparator(SortOption sort) {
        Comparator<ScoredOffer> byValue = Comparator.comparingInt((ScoredOffer s) -> s.value().score()).reversed();
        Comparator<ScoredOffer> byPrice = Comparator.comparingLong(s -> s.offer().getPrice());
        return switch (sort) {
            case BEST_VALUE -> byValue
                    .thenComparing(Comparator.comparingLong((ScoredOffer s) ->
                            Money.saving(s.offer().getPrice(), s.offer().getOriginalPrice())).reversed())
                    .thenComparing(byPrice);
            case BIGGEST_SAVING -> Comparator.comparingLong((ScoredOffer s) ->
                            Money.saving(s.offer().getPrice(), s.offer().getOriginalPrice())).reversed()
                    .thenComparing(byValue);
            case TOP_RATED -> Comparator.comparingDouble(ScoredOffer::effectiveRating).reversed()
                    .thenComparing(byValue);
            case NEAREST -> Comparator.comparing(ScoredOffer::distanceKm,
                            Comparator.nullsLast(Comparator.<Double>naturalOrder()))
                    .thenComparing(byValue);
            case LOWEST_PRICE -> byPrice.thenComparing(byValue);
        };
    }
}
