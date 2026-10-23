package com.shofiniakol.admin;

import com.shofiniakol.admin.DashboardDtos.DailySalesDto;
import com.shofiniakol.admin.DashboardDtos.DashboardOrderDto;
import com.shofiniakol.admin.DashboardDtos.DashboardSummaryDto;
import com.shofiniakol.admin.DashboardDtos.RecentOrderDto;
import com.shofiniakol.admin.DashboardDtos.TopItemDto;
import com.shofiniakol.admin.DashboardDtos.TrendDto;
import com.shofiniakol.catalog.MenuItemRepository;
import com.shofiniakol.catalog.Restaurant;
import com.shofiniakol.catalog.RestaurantRepository;
import com.shofiniakol.common.PageResponse;
import com.shofiniakol.config.AppProperties;
import com.shofiniakol.order.Order;
import com.shofiniakol.order.OrderDtos.OrderDto;
import com.shofiniakol.order.OrderRepository;
import com.shofiniakol.order.OrderService;
import com.shofiniakol.order.OrderStatus;
import com.shofiniakol.user.User;
import com.shofiniakol.user.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final AdminAccess access;
    private final AppProperties props;

    // ------------------------------------------------------------------ home screen

    @Transactional(readOnly = true)
    public DashboardSummaryDto summary(UUID restaurantId) {
        List<UUID> ids = scope(restaurantId);
        if (ids.isEmpty()) {
            return new DashboardSummaryDto(0, new TrendDto(0, true), 0, new TrendDto(0, true), 0, 0, 0,
                    List.of(), List.of(), List.of());
        }
        ZoneId zone = ZoneId.of(props.getTimezone());
        LocalDate today = LocalDate.now(zone);
        Instant startToday = today.atStartOfDay(zone).toInstant();
        Instant startTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant startYesterday = today.minusDays(1).atStartOfDay(zone).toInstant();

        long ordersToday = countActive(startToday, startTomorrow, ids);
        long ordersYesterday = countActive(startYesterday, startToday, ids);
        long revenueToday = delivered(startToday, startTomorrow, ids)[1];
        long revenueYesterday = delivered(startYesterday, startToday, ids)[1];

        // last 7 days, oldest first (the chart's left-to-right order)
        List<DailySalesDto> weekly = new ArrayList<>();
        for (int d = 6; d >= 0; d--) {
            LocalDate day = today.minusDays(d);
            long[] row = delivered(day.atStartOfDay(zone).toInstant(), day.plusDays(1).atStartOfDay(zone).toInstant(), ids);
            weekly.add(new DailySalesDto(day, row[0], row[1]));
        }

        // best sellers of the last 30 days
        List<Object[]> top = orderRepository.topItems(ids, OrderStatus.CANCELLED,
                today.minusDays(29).atStartOfDay(zone).toInstant(), startTomorrow, PageRequest.of(0, 5));
        long best = top.isEmpty() ? 0 : ((Number) top.get(0)[1]).longValue();
        List<TopItemDto> topItems = top.stream().map(r -> {
            long qty = ((Number) r[1]).longValue();
            return new TopItemDto((String) r[0], qty, best == 0 ? 0 : (int) Math.round(qty * 100.0 / best));
        }).toList();

        Page<Order> recent = orderRepository.findByRestaurantIdIn(ids,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<UUID, User> users = usersOf(recent.getContent());
        List<RecentOrderDto> recentOrders = recent.getContent().stream().map(o -> new RecentOrderDto(
                o.getId(), o.getOrderNumber(), nameOf(users.get(o.getUserId())), o.getTotal(), o.getStatus(),
                o.getStatus().label(), o.getCreatedAt())).toList();

        return new DashboardSummaryDto(
                ordersToday, trend(ordersToday, ordersYesterday),
                revenueToday, trend(revenueToday, revenueYesterday),
                orderRepository.countByRestaurantIdInAndStatus(ids, OrderStatus.PENDING),
                menuItemRepository.countByRestaurantIdInAndAvailableTrue(ids),
                orderRepository.countCustomers(ids, OrderStatus.CANCELLED),
                recentOrders, weekly, topItems);
    }

    /** Orders created in [from, to) that were not cancelled. */
    private long countActive(Instant from, Instant to, Collection<UUID> ids) {
        long total = 0;
        for (Object[] r : orderRepository.countByStatusForRestaurants(from, to, ids)) {
            if (!OrderStatus.CANCELLED.equals(r[0])) {
                total += ((Number) r[1]).longValue();
            }
        }
        return total;
    }

    /** [count, revenue] of DELIVERED orders created in [from, to). */
    private long[] delivered(Instant from, Instant to, Collection<UUID> ids) {
        Object[] row = orderRepository.statsForRestaurants(OrderStatus.DELIVERED, from, to, ids).get(0);
        return new long[]{((Number) row[0]).longValue(), ((Number) row[1]).longValue()};
    }

    private static TrendDto trend(long now, long before) {
        if (before == 0) {
            return new TrendDto(now > 0 ? 100 : 0, true);
        }
        return new TrendDto((int) Math.round(Math.abs(now - before) * 100.0 / before), now >= before);
    }

    // ------------------------------------------------------------------ orders screen

    @Transactional(readOnly = true)
    public PageResponse<DashboardOrderDto> orders(UUID restaurantId, OrderStatus status, Integer page, Integer limit) {
        if (restaurantId != null) {
            access.requireRestaurant(restaurantId);
        }
        UUID owner = access.ownerScope(); // null = admin
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            Join<Object, Object> restaurant = root.join("restaurant");
            if (restaurantId != null) {
                p.add(cb.equal(restaurant.get("id"), restaurantId));
            }
            if (owner != null) {
                p.add(cb.equal(restaurant.get("ownerId"), owner));
            }
            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        var pageable = PageRequest.of(PageResponse.normalizePage(page) - 1, PageResponse.normalizeLimit(limit),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> result = orderRepository.findAll(spec, pageable);
        Map<UUID, User> users = usersOf(result.getContent());
        return PageResponse.from(result, o -> toDto(o, users));
    }

    @Transactional(readOnly = true)
    public DashboardOrderDto order(UUID id) {
        Order o = orderService.getEntity(id);
        access.assertCanManage(o.getRestaurant());
        return toDto(o, usersOf(List.of(o)));
    }

    private DashboardOrderDto toDto(Order o, Map<UUID, User> users) {
        OrderDto d = orderService.toDto(o); // items, payment and timeline are mapped exactly like the app does
        User customer = users.get(o.getUserId());
        List<OrderStatus> next = Arrays.stream(OrderStatus.values())
                .filter(s -> o.getStatus().canMoveTo(s, o.getDeliveryType())).toList();
        return new DashboardOrderDto(o.getId(), o.getOrderNumber(), o.getStatus(), o.getStatus().label(),
                o.getDeliveryType(), next, o.getRestaurant().getId(), o.getRestaurant().getName(),
                o.getUserId(), nameOf(customer), o.getContactPhone(),
                d.deliveryAddress(), d.deliveryLatitude(), d.deliveryLongitude(), d.driverNotes(),
                d.items(), o.getSubtotal(), o.getDeliveryFee(), o.getTotal(), o.getSavingTotal(),
                d.payment(), d.estimatedMinutes(), d.cancelledReason(), d.timeline(),
                o.getCreatedAt(), o.getDeliveredAt());
    }

    // ------------------------------------------------------------------ helpers

    /** Restaurants the caller may look at: one given id, else all of the owner's (admin: every restaurant). */
    private List<UUID> scope(UUID restaurantId) {
        if (restaurantId != null) {
            access.requireRestaurant(restaurantId);
            return List.of(restaurantId);
        }
        List<UUID> own = access.restaurantScope();
        if (own != null) {
            return own;
        }
        return restaurantRepository.findAll().stream().map(Restaurant::getId).toList();
    }

    private Map<UUID, User> usersOf(Collection<Order> orders) {
        List<UUID> userIds = orders.stream().map(Order::getUserId).distinct().toList();
        Map<UUID, User> map = new HashMap<>();
        userRepository.findAllById(userIds).forEach(u -> map.put(u.getId(), u));
        return map;
    }

    private static String nameOf(User u) {
        return u == null || u.getFullName() == null || u.getFullName().isBlank() ? null : u.getFullName();
    }
}
