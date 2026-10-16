package com.shofiniakol.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderLineRequest(
            @NotNull(message = "offer_id مطلوب") UUID offerId,
            @NotNull(message = "الكمية مطلوبة") @Min(value = 1, message = "الكمية يجب أن تكون 1 على الأقل")
            @Max(value = 20, message = "الحد الأقصى 20 لكل عنصر") Integer quantity,
            List<UUID> addonIds,
            @Size(max = 300, message = "الملاحظة طويلة جداً") String note) {
    }

    /**
     * Checkout payload. For delivery send address_id; for Sham Cash send payment_reference
     * (the transfer's transaction number). client_request_id makes retries idempotent.
     */
    public record CreateOrderRequest(
            @NotEmpty(message = "السلة فارغة") @Size(max = 20, message = "عدد العناصر كبير جداً")
            List<@Valid OrderLineRequest> items,
            @NotNull(message = "اختر طريقة الاستلام") DeliveryType deliveryType,
            UUID addressId,
            @NotNull(message = "اختر طريقة الدفع") PaymentMethod paymentMethod,
            @Size(max = 100, message = "رقم العملية طويل جداً") String paymentReference,
            @Size(max = 500, message = "الملاحظات طويلة جداً") String driverNotes,
            @Size(max = 64) String clientRequestId) {
    }

    public record CancelRequest(@Size(max = 300, message = "السبب طويل جداً") String reason) {
    }

    public record AddonLineDto(String name, long extraPrice) {
    }

    public record OrderItemDto(UUID offerId, String title, String imageUrl, long unitPrice, long addonsTotal,
                               int quantity, long lineTotal, String note, List<AddonLineDto> addons) {
    }

    public record PaymentDto(PaymentMethod method, PaymentStatus status, String reference, Instant paidAt) {
    }

    public record TimelineDto(OrderStatus status, String label, String note, Instant at) {
    }

    public record OrderRestaurantDto(UUID id, String name, String logoUrl, String phone) {
    }

    public record OrderDto(UUID id, String orderNumber, OrderStatus status, String statusLabel, String tab,
                           OrderRestaurantDto restaurant, DeliveryType deliveryType,
                           String deliveryAddress, Double deliveryLatitude, Double deliveryLongitude,
                           String contactPhone, List<OrderItemDto> items,
                           long subtotal, long deliveryFee, long total, long savingTotal,
                           PaymentDto payment, String driverNotes, Integer estimatedMinutes,
                           String cancelledReason, boolean canCancel, boolean canReview, boolean reviewed,
                           List<TimelineDto> timeline, Instant createdAt, Instant deliveredAt) {
    }

    /** Lightweight row for the "My orders" tabs. */
    public record OrderSummaryDto(UUID id, String orderNumber, OrderStatus status, String statusLabel, String tab,
                                  String restaurantName, String restaurantLogoUrl, int itemsCount,
                                  String itemsPreview, long total, Instant createdAt) {
    }
}
