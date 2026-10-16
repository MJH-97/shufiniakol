package com.shofiniakol.discover;

import com.shofiniakol.catalog.AvailabilityService;
import com.shofiniakol.catalog.Category;
import com.shofiniakol.catalog.CategoryService;
import com.shofiniakol.catalog.DeliveryPricingService;
import com.shofiniakol.catalog.DeliveryPricingService.DeliveryQuote;
import com.shofiniakol.catalog.MenuItemRepository;
import com.shofiniakol.catalog.Restaurant;
import com.shofiniakol.catalog.RestaurantRepository;
import com.shofiniakol.common.ApiException;
import com.shofiniakol.common.GeoUtils;
import com.shofiniakol.common.PageResponse;
import com.shofiniakol.discover.CatalogDtos.MenuItemDto;
import com.shofiniakol.discover.CatalogDtos.OfferSummaryDto;
import com.shofiniakol.discover.CatalogDtos.RestaurantCardDto;
import com.shofiniakol.discover.CatalogDtos.RestaurantDetailDto;
import com.shofiniakol.favorite.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final CategoryService categoryService;
    private final OfferRankingService ranking;
    private final OfferMapper mapper;
    private final DeliveryPricingService delivery;
    private final AvailabilityService availability;
    private final FavoriteService favorites;

    /** Restaurants around the user with the best offer each one has inside the budget. */
    @Transactional(readOnly = true)
    public PageResponse<RestaurantCardDto> nearby(Double lat, Double lng, Double radiusKm, Long budget,
                                                  String category, Integer page, Integer limit, UUID userId) {
        if (!GeoUtils.validLatLng(lat, lng)) {
            throw ApiException.badRequest("LOCATION_REQUIRED", "حدد موقعك لعرض المطاعم القريبة");
        }
        double radius = radiusKm == null || radiusKm <= 0 ? 10 : Math.min(radiusKm, 50);
        Category cat = categoryService.resolve(category);
        Set<UUID> favRestaurants = favorites.restaurantIds(userId);
        Set<UUID> favOffers = favorites.offerIds(userId);

        Map<UUID, OfferSummaryDto> bestByRestaurant = bestOffers(budget, cat == null ? null : cat.getId(),
                lat, lng, favOffers);
        Instant now = Instant.now();
        List<RestaurantCardDto> cards = new ArrayList<>();
        for (Restaurant r : restaurantRepository.findByActiveTrue()) {
            double d = GeoUtils.distanceKm(lat, lng, r.getLatitude(), r.getLongitude());
            if (d > radius) {
                continue;
            }
            if (cat != null && r.getCategories().stream().noneMatch(c -> c.getId().equals(cat.getId()))) {
                continue;
            }
            OfferSummaryDto best = bestByRestaurant.get(r.getId());
            if (budget != null && best == null) {
                continue;
            }
            cards.add(card(r, d, favRestaurants.contains(r.getId()), best, now));
        }
        cards.sort(Comparator.comparing(RestaurantCardDto::distanceKm));
        return PageResponse.of(cards, page, limit);
    }

    @Transactional(readOnly = true)
    public RestaurantDetailDto detail(UUID id, Double lat, Double lng, UUID userId) {
        Restaurant r = restaurantRepository.findById(id)
                .filter(Restaurant::isActive)
                .orElseThrow(() -> ApiException.notFound("RESTAURANT_NOT_FOUND", "المطعم غير موجود"));
        Double d = GeoUtils.validLatLng(lat, lng)
                ? GeoUtils.distanceKm(lat, lng, r.getLatitude(), r.getLongitude()) : null;
        Set<UUID> favOffers = favorites.offerIds(userId);

        List<ScoredOffer> scored = ranking.find(new OfferRankingService.Query(null, false, null, id, lat, lng,
                null, null, null, false, false), SortOption.BEST_VALUE);
        List<OfferSummaryDto> offers = scored.stream().map(s -> mapper.summary(s, favOffers)).toList();
        List<MenuItemDto> menu = menuItemRepository.findByRestaurantIdAndAvailableTrueOrderBySortOrderAscNameAsc(id)
                .stream().map(m -> new MenuItemDto(m.getId(), m.getName(), m.getDescription(), m.getPrice(),
                        m.getImageUrl(), m.isAvailable())).toList();
        RestaurantCardDto card = card(r, d, favorites.restaurantIds(userId).contains(id),
                offers.isEmpty() ? null : offers.get(0), Instant.now());
        return new RestaurantDetailDto(card, r.getDescription(), r.getPhone(), r.getOpensAt(), r.getClosesAt(),
                offers, menu);
    }

    public RestaurantCardDto card(Restaurant r, Double distanceKm, boolean favorite, OfferSummaryDto best, Instant now) {
        DeliveryQuote q = delivery.quote(r, distanceKm);
        return new RestaurantCardDto(r.getId(), r.getName(), r.getLogoUrl(), r.getCoverUrl(), r.getArea(),
                r.getAddressLine(), r.getLatitude(), r.getLongitude(),
                GeoUtils.round1(r.getRatingAvg()), r.getRatingCount(),
                distanceKm == null ? null : GeoUtils.round1(distanceKm),
                availability.isRestaurantOpen(r, now), r.isAcceptingOrders(),
                q.fee(), q.deliverable(), q.etaMinutes(), q.minOrderAmount(), favorite,
                r.getCategories().stream().map(mapper::category).toList(), best);
    }

    /** First (= best value) offer per restaurant. */
    Map<UUID, OfferSummaryDto> bestOffers(Long budget, UUID categoryId, Double lat, Double lng, Set<UUID> favOffers) {
        List<ScoredOffer> scored = ranking.find(new OfferRankingService.Query(budget, false, categoryId, null,
                lat, lng, null, null, null, false, false), SortOption.BEST_VALUE);
        Map<UUID, OfferSummaryDto> best = new HashMap<>();
        for (ScoredOffer s : scored) {
            best.putIfAbsent(s.offer().getRestaurant().getId(), mapper.summary(s, favOffers));
        }
        return best;
    }
}
