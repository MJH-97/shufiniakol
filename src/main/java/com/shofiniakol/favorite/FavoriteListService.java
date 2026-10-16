package com.shofiniakol.favorite;

import com.shofiniakol.alert.PriceAlertService;
import com.shofiniakol.alert.PriceAlertService.PriceAlertDto;
import com.shofiniakol.catalog.Offer;
import com.shofiniakol.catalog.OfferRepository;
import com.shofiniakol.catalog.Restaurant;
import com.shofiniakol.catalog.RestaurantRepository;
import com.shofiniakol.common.GeoUtils;
import com.shofiniakol.discover.CatalogDtos.OfferSummaryDto;
import com.shofiniakol.discover.CatalogDtos.RestaurantCardDto;
import com.shofiniakol.discover.OfferMapper;
import com.shofiniakol.discover.OfferRankingService;
import com.shofiniakol.discover.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Builds the "المحفوظات" screen: saved meals, saved restaurants and the user's price alerts. */
@Service
@RequiredArgsConstructor
public class FavoriteListService {

    private final FavoriteRepository repo;
    private final OfferRepository offerRepository;
    private final RestaurantRepository restaurantRepository;
    private final OfferRankingService ranking;
    private final OfferMapper mapper;
    private final RestaurantService restaurantService;
    private final PriceAlertService alertService;

    public record FavoriteOfferDto(UUID favoriteId, Instant savedAt, OfferSummaryDto offer) {
    }

    public record FavoriteRestaurantDto(UUID favoriteId, Instant savedAt, RestaurantCardDto restaurant) {
    }

    public record FavoritesResponse(List<FavoriteOfferDto> offers, List<FavoriteRestaurantDto> restaurants,
                                    List<PriceAlertDto> priceAlerts) {
    }

    @Transactional(readOnly = true)
    public FavoritesResponse list(UUID userId, Double lat, Double lng) {
        List<Favorite> favs = repo.findByUserIdOrderByCreatedAtDesc(userId);

        Map<UUID, Offer> offers = offerRepository.findAllById(
                favs.stream().map(Favorite::getOfferId).filter(java.util.Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(Offer::getId, Function.identity()));
        Map<UUID, Restaurant> restaurants = restaurantRepository.findAllById(
                favs.stream().map(Favorite::getRestaurantId).filter(java.util.Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(Restaurant::getId, Function.identity()));

        Set<UUID> favOfferIds = offers.keySet();
        List<FavoriteOfferDto> offerDtos = new ArrayList<>();
        List<FavoriteRestaurantDto> restaurantDtos = new ArrayList<>();
        Instant now = Instant.now();
        for (Favorite f : favs) {
            if (f.getOfferId() != null && offers.containsKey(f.getOfferId())) {
                var scored = ranking.scoreOne(offers.get(f.getOfferId()), null, lat, lng);
                offerDtos.add(new FavoriteOfferDto(f.getId(), f.getCreatedAt(), mapper.summary(scored, favOfferIds)));
            } else if (f.getRestaurantId() != null && restaurants.containsKey(f.getRestaurantId())) {
                Restaurant r = restaurants.get(f.getRestaurantId());
                Double d = GeoUtils.validLatLng(lat, lng)
                        ? GeoUtils.distanceKm(lat, lng, r.getLatitude(), r.getLongitude()) : null;
                restaurantDtos.add(new FavoriteRestaurantDto(f.getId(), f.getCreatedAt(),
                        restaurantService.card(r, d, true, null, now)));
            }
        }
        return new FavoritesResponse(offerDtos, restaurantDtos, alertService.list(userId));
    }
}
