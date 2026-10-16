package com.shofiniakol.discover;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Response shapes of the public catalog API (serialised in snake_case). */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CategoryDto(UUID id, String slug, String name, String emoji, String iconUrl) {
    }

    public record RestaurantRefDto(UUID id, String name, String logoUrl, String area, double rating,
                                   int ratingCount, boolean open) {
    }

    public record OfferSummaryDto(UUID id, String title, String description, String imageUrl,
                                  long price, long originalPrice, int discountPercentage, long savingAmount,
                                  double rating, int ratingCount, Double distanceKm,
                                  long deliveryFee, boolean freeDelivery, boolean deliverable,
                                  int estimatedDeliveryMinutes, int valueScore, String valueLabel,
                                  int itemsCount, Instant endsAt, boolean limitedTime, boolean sponsored,
                                  boolean favorite, boolean available,
                                  RestaurantRefDto restaurant, CategoryDto category) {
    }

    public record OfferItemDto(UUID id, String name, int quantity) {
    }

    public record AddonDto(UUID id, String name, long extraPrice) {
    }

    public record ValueComponentDto(String key, String label, int score) {
    }

    public record PricePointDto(Instant at, long price) {
    }

    public record AvailabilityDto(int dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }

    public record AlertRefDto(UUID id, long targetPrice, boolean triggered) {
    }

    public record OfferDetailDto(UUID id, String title, String description, String imageUrl, List<String> images,
                                 long price, long originalPrice, int discountPercentage, long savingAmount,
                                 double rating, int ratingCount, Double distanceKm,
                                 long deliveryFee, boolean freeDelivery, boolean deliverable,
                                 int estimatedDeliveryMinutes, long minOrderAmount,
                                 int valueScore, String valueLabel, List<String> valueReasons,
                                 List<ValueComponentDto> valueBreakdown,
                                 int itemsCount, int servings, int maxAddons, Integer stockRemaining,
                                 Instant startsAt, Instant endsAt, boolean limitedTime, boolean sponsored,
                                 boolean favorite, boolean available,
                                 List<OfferItemDto> items, List<AddonDto> addons,
                                 List<AvailabilityDto> availability,
                                 Long lowestPrice30d, List<PricePointDto> priceHistory,
                                 AlertRefDto priceAlert,
                                 RestaurantCardDto restaurant, CategoryDto category) {
    }

    public record RestaurantCardDto(UUID id, String name, String logoUrl, String coverUrl, String area,
                                    String addressLine, double latitude, double longitude,
                                    double rating, int ratingCount, Double distanceKm,
                                    boolean open, boolean acceptingOrders,
                                    long deliveryFee, boolean deliverable, int estimatedDeliveryMinutes,
                                    long minOrderAmount, boolean favorite,
                                    List<CategoryDto> categories, OfferSummaryDto bestOffer) {
    }

    public record MenuItemDto(UUID id, String name, String description, long price, String imageUrl,
                              boolean available) {
    }

    public record RestaurantDetailDto(RestaurantCardDto restaurant, String description, String phone,
                                      LocalTime opensAt, LocalTime closesAt,
                                      List<OfferSummaryDto> offers, List<MenuItemDto> menu) {
    }
}
