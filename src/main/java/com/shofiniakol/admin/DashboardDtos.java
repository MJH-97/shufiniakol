package com.shofiniakol.admin;

import com.shofiniakol.order.DeliveryType;
import com.shofiniakol.order.OrderDtos.OrderItemDto;
import com.shofiniakol.order.OrderDtos.PaymentDto;
import com.shofiniakol.order.OrderDtos.TimelineDto;
import com.shofiniakol.order.OrderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Response shapes of the restaurant dashboard's home screen and orders screen (read-only). */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    /** Change versus yesterday, e.g. {value: 12, up: true} = "+12%". */
    public record TrendDto(int value, boolean up) {
    }

    public record RecentOrderDto(UUID id, String orderNumber, String customerName, long total,
                                 OrderStatus status, String statusLabel, Instant createdAt) {
    }

    /** One bar of the weekly chart. revenue = total of DELIVERED orders created that day. */
    public record DailySalesDto(LocalDate date, long orders, long revenue) {
    }

    /** percent is relative to the best seller (100 = best seller), ready to use as a bar width. */
    public record TopItemDto(String title, long quantity, int percent) {
    }

    public record DashboardSummaryDto(long ordersToday, TrendDto ordersTrend,
                                      long revenueToday, TrendDto revenueTrend,
                                      long pendingOrders, long activeMenuItems, long customers,
                                      List<RecentOrderDto> recentOrders,
                                      List<DailySalesDto> weeklySales,
                                      List<TopItemDto> topItems) {
    }

    /**
     * Order as the dashboard needs it: customer name, and next_statuses = the statuses the restaurant may move
     * the order to right now (the backend owns the lifecycle, pickup orders end with READY_FOR_PICKUP).
     */
    public record DashboardOrderDto(UUID id, String orderNumber, OrderStatus status, String statusLabel,
                                    DeliveryType deliveryType, List<OrderStatus> nextStatuses,
                                    UUID restaurantId, String restaurantName,
                                    UUID customerId, String customerName, String customerPhone,
                                    String deliveryAddress, Double deliveryLatitude, Double deliveryLongitude,
                                    String driverNotes,
                                    List<OrderItemDto> items,
                                    long subtotal, long deliveryFee, long total, long savingTotal,
                                    PaymentDto payment, Integer estimatedMinutes, String cancelledReason,
                                    List<TimelineDto> timeline, Instant createdAt, Instant deliveredAt) {
    }
}
