package com.shofiniakol.discover;

import com.shofiniakol.alert.PriceAlertRepository;
import com.shofiniakol.catalog.Category;
import com.shofiniakol.catalog.CategoryService;
import com.shofiniakol.catalog.Offer;
import com.shofiniakol.catalog.OfferAddon;
import com.shofiniakol.catalog.OfferRepository;
import com.shofiniakol.catalog.PriceHistory;
import com.shofiniakol.catalog.PriceHistoryRepository;
import com.shofiniakol.catalog.Restaurant;
import com.shofiniakol.common.ApiException;
import com.shofiniakol.common.GeoUtils;
import com.shofiniakol.common.Money;
import com.shofiniakol.common.PageResponse;
import com.shofiniakol.discover.CatalogDtos.AddonDto;
import com.shofiniakol.discover.CatalogDtos.AlertRefDto;
import com.shofiniakol.discover.CatalogDtos.AvailabilityDto;
import com.shofiniakol.discover.CatalogDtos.OfferDetailDto;
import com.shofiniakol.discover.CatalogDtos.OfferItemDto;
import com.shofiniakol.discover.CatalogDtos.OfferSummaryDto;
import com.shofiniakol.discover.CatalogDtos.PricePointDto;
import com.shofiniakol.discover.CatalogDtos.ValueComponentDto;
import com.shofiniakol.favorite.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PriceAlertRepository alertRepository;
    private final OfferRankingService ranking;
    private final OfferMapper mapper;
    private final FavoriteService favorites;
    private final CategoryService categoryService;
    private final RestaurantService restaurantService;

    /** filter: all | today | limited. */
    @Transactional(readOnly = true)
    public PageResponse<OfferSummaryDto> list(String filter, String category, UUID restaurantId, String sort,
                                              Double lat, Double lng, Long minPrice, Long maxPrice,
                                              Integer page, Integer limit, UUID userId) {
        Category cat = categoryService.resolve(category);
        boolean limited = "limited".equalsIgnoreCase(filter);
        var query = new OfferRankingService.Query(maxPrice, false, cat == null ? null : cat.getId(), restaurantId,
                lat, lng, null, null, minPrice, false, limited);
        var favs = favorites.offerIds(userId);
        List<OfferSummaryDto> all = ranking.find(query, SortOption.parse(sort)).stream()
                .map(s -> mapper.summary(s, favs)).toList();
        return PageResponse.of(all, page, limit);
    }

    /** Not read-only: bumps the view counter used by the dashboard statistics. */
    @Transactional
    public OfferDetailDto detail(UUID id, Long budget, Double lat, Double lng, UUID userId) {
        Offer o = offerRepository.findById(id)
                .filter(x -> x.isActive() && x.getRestaurant().isActive())
                .orElseThrow(() -> ApiException.notFound("OFFER_NOT_FOUND", "العرض غير موجود"));
        offerRepository.incrementViews(id);

        ScoredOffer s = ranking.scoreOne(o, budget, lat, lng);
        Restaurant r = o.getRestaurant();
        Instant since = Instant.now().minus(Duration.ofDays(30));
        List<PriceHistory> history = priceHistoryRepository.findByOfferIdAndCreatedAtAfterOrderByCreatedAtAsc(id, since);
        Long lowest = history.stream().map(PriceHistory::getPrice).min(Long::compare).orElse(null);
        if (lowest == null || o.getPrice() < lowest) {
            lowest = o.getPrice();
        }
        AlertRefDto alert = userId == null ? null : alertRepository.findByUserIdAndOfferId(userId, id)
                .map(a -> new AlertRefDto(a.getId(), a.getTargetPrice(), a.getTriggeredAt() != null)).orElse(null);
        boolean fav = favorites.offerIds(userId).contains(id);
        boolean restaurantFav = favorites.restaurantIds(userId).contains(r.getId());

        List<ValueComponentDto> breakdown = s.value().components().stream()
                .map(c -> new ValueComponentDto(c.key(), ValueText.componentLabel(c.key()),
                        (int) Math.round(c.score() * 100)))
                .toList();
        long price = o.getPrice();
        return new OfferDetailDto(o.getId(), o.getTitle(), o.getDescription(), o.getImageUrl(),
                List.copyOf(o.getImages()), price, o.getOriginalPrice(),
                Money.discountPercent(price, o.getOriginalPrice()), Money.saving(price, o.getOriginalPrice()),
                GeoUtils.round1(s.effectiveRating()), OfferMapper.displayRatingCount(o, r),
                s.distanceKm() == null ? null : GeoUtils.round1(s.distanceKm()),
                s.quote().fee(), s.quote().fee() == 0, s.quote().deliverable(), s.quote().etaMinutes(),
                s.quote().minOrderAmount(),
                s.value().score(), ValueText.label(s.value().score()), ValueText.reasons(s, budget), breakdown,
                o.getItemsCount(), o.getServings(), o.getMaxAddons(), o.getStockRemaining(),
                o.getStartsAt(), o.getEndsAt(), s.limitedTime(), o.isSponsored(), fav, s.available(),
                o.getItems().stream().map(i -> new OfferItemDto(i.getId(), i.getName(), i.getQuantity())).toList(),
                o.getAddons().stream().filter(OfferAddon::isAvailable)
                        .map(a -> new AddonDto(a.getId(), a.getName(), a.getExtraPrice())).toList(),
                o.getAvailability().stream()
                        .map(a -> new AvailabilityDto(a.getDayOfWeek(), a.getStartTime(), a.getEndTime())).toList(),
                lowest, history.stream().map(h -> new PricePointDto(h.getCreatedAt(), h.getPrice())).toList(),
                alert, restaurantService.card(r, s.distanceKm(), restaurantFav, null, Instant.now()),
                mapper.category(o.getCategory()));
    }

    public List<PricePointDto> priceHistory(UUID id, int days) {
        int d = Math.max(1, Math.min(days, 365));
        return priceHistoryRepository.findByOfferIdAndCreatedAtAfterOrderByCreatedAtAsc(id,
                        Instant.now().minus(Duration.ofDays(d)))
                .stream().map(h -> new PricePointDto(h.getCreatedAt(), h.getPrice())).toList();
    }
}
