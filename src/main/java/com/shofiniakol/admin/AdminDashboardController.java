package com.shofiniakol.admin;

import com.shofiniakol.admin.DashboardDtos.DashboardOrderDto;
import com.shofiniakol.admin.DashboardDtos.DashboardSummaryDto;
import com.shofiniakol.common.PageResponse;
import com.shofiniakol.order.OrderStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read endpoints of the restaurant-owner dashboard. Owners see only their own restaurants, admins see all
 * (same rules as the rest of /api/v1/admin/**). Changing an order stays on PATCH /admin/orders/{id}/status.
 */
@Tag(name = "Admin - Dashboard")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService service;

    /** Home screen: today's numbers, recent orders, last-7-days sales and best sellers. */
    @GetMapping("/summary")
    public DashboardSummaryDto summary(@RequestParam(name = "restaurant_id", required = false) UUID restaurantId) {
        return service.summary(restaurantId);
    }

    /** Orders screen: newest first, optional status filter. */
    @GetMapping("/orders")
    public PageResponse<DashboardOrderDto> orders(@RequestParam(name = "restaurant_id", required = false) UUID restaurantId,
                                                  @RequestParam(required = false) OrderStatus status,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer limit) {
        return service.orders(restaurantId, status, page, limit);
    }

    @GetMapping("/orders/{id}")
    public DashboardOrderDto order(@PathVariable UUID id) {
        return service.order(id);
    }
}
