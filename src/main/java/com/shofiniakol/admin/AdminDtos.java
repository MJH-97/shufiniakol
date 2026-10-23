package com.shofiniakol.admin;

import com.shofiniakol.discover.CatalogDtos.CategoryDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Request/response shapes of the restaurant & admin dashboard API. */
public final class AdminDtos {

    private AdminDtos() {
    }

    // ---------------- restaurants ----------------
    /** Partial update used by the settings screen: only the fields that are sent are changed (send "" to clear text). */
    public record RestaurantPatchRequest(
            @Size(max = 120) String name,
            @Size(max = 1000) String description,
            @Size(max = 500) String logoUrl,
            @Size(max = 500) String coverUrl,
            @Size(max = 30) String phone,
            @Size(max = 80) String city,
            @Size(max = 80) String area,
            @Size(max = 250) String addressLine,
            @Min(-90) @Max(90) Double latitude,
            @Min(-180) @Max(180) Double longitude,
            LocalTime opensAt, LocalTime closesAt,
            Boolean acceptingOrders,
            @PositiveOrZero Long defaultDeliveryFee,
            @PositiveOrZero Long minOrderAmount,
            @Positive Integer avgPrepMinutes) {
    }

    public record RestaurantRequest(
            @NotBlank(message = "اسم المطعم مطلوب") @Size(max = 120) String name,
            @Size(max = 1000) String description,
            @Size(max = 500) String logoUrl,
            @Size(max = 500) String coverUrl,
            @Size(max = 30) String phone,
            @Size(max = 80) String city,
            @Size(max = 80) String area,
            @Size(max = 250) String addressLine,
            @NotNull(message = "خط العرض مطلوب") @Min(-90) @Max(90) Double latitude,
            @NotNull(message = "خط الطول مطلوب") @Min(-180) @Max(180) Double longitude,
            LocalTime opensAt, LocalTime closesAt,
            Boolean acceptingOrders,
            @PositiveOrZero Long defaultDeliveryFee,
            @PositiveOrZero Long minOrderAmount,
            @Positive Integer avgPrepMinutes,
            Set<UUID> categoryIds,
            // ---- admin only (ignored for restaurant owners) ----
            String ownerPhone, Boolean active, @PositiveOrZero @Max(100) Double commissionPercent) {
    }

    public record AdminRestaurantDto(UUID id, UUID ownerId, String ownerPhone, String name, String description,
                                     String logoUrl, String coverUrl, String phone, String city, String area,
                                     String addressLine, double latitude, double longitude,
                                     LocalTime opensAt, LocalTime closesAt, boolean acceptingOrders, boolean active,
                                     long defaultDeliveryFee, long minOrderAmount, int avgPrepMinutes,
                                     double commissionPercent, double rating, int ratingCount,
                                     List<CategoryDto> categories, Instant createdAt) {
    }

    // ---------------- categories ----------------
    public record CategoryRequest(
            @NotBlank @Size(max = 40) String slug,
            @NotBlank @Size(max = 80) String nameAr,
            @NotBlank @Size(max = 80) String nameEn,
            @NotBlank @Size(max = 80) String nameFr,
            @Size(max = 16) String emoji, @Size(max = 300) String iconUrl,
            Integer sortOrder, Boolean active) {
    }

    // ---------------- menu & zones ----------------
    public record MenuItemRequest(
            @NotBlank(message = "اسم الصنف مطلوب") @Size(max = 150) String name,
            @Size(max = 600) String description,
            @NotNull(message = "السعر مطلوب") @PositiveOrZero Long price,
            @Size(max = 500) String imageUrl, Boolean available, Integer sortOrder, UUID categoryId,
            /** Optional: several categories per item. When sent it wins over category_id (first = main category). */
            List<UUID> categoryIds) {
    }

    public record MenuItemAdminDto(UUID id, String name, String description, long price, String imageUrl,
                                   boolean available, int sortOrder, UUID categoryId, List<UUID> categoryIds) {
    }

    public record ZoneRequest(
            @NotBlank(message = "اسم المنطقة مطلوب") @Size(max = 100) String name,
            @NotNull @Positive Double maxDistanceKm,
            @NotNull @PositiveOrZero Long fee,
            @PositiveOrZero Long minOrderAmount,
            @Positive Integer estimatedMinutes, Boolean active) {
    }

    public record ZoneDto(UUID id, String name, double maxDistanceKm, long fee, long minOrderAmount,
                          int estimatedMinutes, boolean active) {
    }

    // ---------------- offers ----------------
    public record OfferItemRequest(@NotBlank @Size(max = 150) String name, @Min(1) @Max(50) Integer quantity,
                                   UUID menuItemId) {
    }

    public record OfferAddonRequest(@NotBlank @Size(max = 120) String name, @PositiveOrZero Long extraPrice,
                                    Boolean available) {
    }

    public record AvailabilityRequest(@NotNull @Min(1) @Max(7) Integer dayOfWeek, @NotNull LocalTime startTime,
                                      @NotNull LocalTime endTime) {
    }

    public record OfferRequest(
            @NotNull(message = "المطعم مطلوب") UUID restaurantId,
            UUID categoryId, // optional: derived from the linked menu items / the restaurant when omitted
            @NotBlank(message = "عنوان العرض مطلوب") @Size(max = 150) String title,
            @Size(max = 1000) String description,
            @Size(max = 500) String imageUrl,
            List<@Size(max = 500) String> images,
            @NotNull(message = "السعر مطلوب") @Positive(message = "السعر يجب أن يكون أكبر من صفر") Long price,
            @NotNull(message = "السعر الأصلي مطلوب") @Positive Long originalPrice,
            @Min(1) Integer servings,
            @Min(0) @Max(20) Integer maxAddons,
            @PositiveOrZero Integer stockRemaining,
            Boolean unlimitedStock,
            Instant startsAt, Instant endsAt,
            Boolean active, Boolean sponsored,
            @Valid List<@Valid OfferItemRequest> items,
            @Valid List<@Valid OfferAddonRequest> addons,
            @Valid List<@Valid AvailabilityRequest> availability) {
    }

    public record OfferAdminDto(UUID id, UUID restaurantId, String restaurantName, UUID categoryId, String title,
                                String description, String imageUrl, List<String> images,
                                long price, long originalPrice, int discountPercentage, int itemsCount, int servings,
                                int maxAddons, Integer stockRemaining, Instant startsAt, Instant endsAt,
                                boolean active, boolean sponsored, double rating, int ratingCount,
                                long viewCount, long orderCount,
                                List<OfferItemAdminDto> items, List<OfferAddonAdminDto> addons,
                                List<AvailabilityRequest> availability, Instant createdAt) {
    }

    public record OfferItemAdminDto(UUID id, String name, int quantity, UUID menuItemId) {
    }

    public record OfferAddonAdminDto(UUID id, String name, long extraPrice, boolean available) {
    }

    // ---------------- orders ----------------
    public record StatusUpdateRequest(@NotNull(message = "الحالة مطلوبة") com.shofiniakol.order.OrderStatus status,
                                      @Size(max = 300) String note) {
    }

    public record PaymentUpdateRequest(@NotNull(message = "الحالة مطلوبة") com.shofiniakol.order.PaymentStatus status) {
    }

    // ---------------- users ----------------
    public record UserUpdateRequest(com.shofiniakol.user.Role role, com.shofiniakol.user.UserStatus status) {
    }

    // ---------------- stats ----------------
    public record TopOfferDto(UUID id, String title, String restaurantName, long orderCount, long viewCount) {
    }

    public record StatsDto(Instant from, Instant to, long deliveredOrders, long revenue, long commission,
                           long customerSavings, long averageOrderValue,
                           java.util.Map<String, Long> ordersByStatus,
                           List<TopOfferDto> topOrderedOffers, List<TopOfferDto> topViewedOffers) {
    }
}
