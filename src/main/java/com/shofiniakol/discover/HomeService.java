package com.shofiniakol.discover;

import com.shofiniakol.catalog.CategoryService;
import com.shofiniakol.common.GeoUtils;
import com.shofiniakol.config.AppProperties;
import com.shofiniakol.discover.CatalogDtos.CategoryDto;
import com.shofiniakol.discover.CatalogDtos.OfferSummaryDto;
import com.shofiniakol.discover.CatalogDtos.RestaurantCardDto;
import com.shofiniakol.favorite.FavoriteService;
import com.shofiniakol.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HomeService {

    private static final int SECTION_SIZE = 10;

    private final OfferRankingService ranking;
    private final OfferMapper mapper;
    private final CategoryService categoryService;
    private final FavoriteService favorites;
    private final RestaurantService restaurantService;
    private final NotificationService notifications;
    private final AppProperties props;

    public record HomeResponse(List<Long> budgetPresets, List<CategoryDto> categories,
                               List<OfferSummaryDto> topOffers, List<OfferSummaryDto> endingSoon,
                               List<RestaurantCardDto> nearbyRestaurants, long unreadNotifications) {
    }

    @Transactional(readOnly = true)
    public HomeResponse home(Double lat, Double lng, Long budget, UUID userId) {
        var favs = favorites.offerIds(userId);
        boolean located = GeoUtils.validLatLng(lat, lng);

        List<OfferSummaryDto> top = ranking.find(new OfferRankingService.Query(budget, false, null, null, lat, lng,
                        null, null, null, false, false), SortOption.BEST_VALUE)
                .stream().limit(SECTION_SIZE).map(s -> mapper.summary(s, favs)).toList();

        List<OfferSummaryDto> ending = ranking.find(new OfferRankingService.Query(budget, false, null, null, lat, lng,
                        null, null, null, false, true), SortOption.BEST_VALUE)
                .stream().sorted(Comparator.comparing(s -> s.offer().getEndsAt()))
                .limit(SECTION_SIZE).map(s -> mapper.summary(s, favs)).toList();

        List<RestaurantCardDto> nearby = located
                ? restaurantService.nearby(lat, lng, 10.0, budget, null, 1, SECTION_SIZE, userId).items()
                : List.of();

        long unread = userId == null ? 0 : notifications.unreadCount(userId);
        return new HomeResponse(props.getDiscover().getBudgetPresets(),
                categoryService.active().stream().map(mapper::category).toList(), top, ending, nearby, unread);
    }
}
