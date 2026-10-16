package com.shofiniakol.admin;

import com.shofiniakol.admin.AdminDtos.StatsDto;
import com.shofiniakol.admin.AdminDtos.TopOfferDto;
import com.shofiniakol.admin.AdminDtos.UserUpdateRequest;
import com.shofiniakol.catalog.Offer;
import com.shofiniakol.catalog.OfferRepository;
import com.shofiniakol.common.ApiException;
import com.shofiniakol.common.PageResponse;
import com.shofiniakol.order.OrderRepository;
import com.shofiniakol.order.OrderStatus;
import com.shofiniakol.user.Role;
import com.shofiniakol.user.User;
import com.shofiniakol.user.UserDto;
import com.shofiniakol.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminMiscService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OfferRepository offerRepository;
    private final AdminAccess access;

    // ---------------------------------------------------------------- users (admin only)

    @Transactional(readOnly = true)
    public PageResponse<UserDto> users(String q, Role role, Integer page, Integer limit) {
        var pageable = PageRequest.of(PageResponse.normalizePage(page) - 1, PageResponse.normalizeLimit(limit),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        String query = q == null ? "" : q.trim();
        var result = role == null ? userRepository.searchAll(query, pageable)
                : userRepository.searchByRole(role, query, pageable);
        return PageResponse.from(result, UserDto::from);
    }

    @Transactional
    public UserDto updateUser(UUID id, UserUpdateRequest req) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "المستخدم غير موجود"));
        if (id.equals(access.currentUserId()) && (req.role() != null || req.status() != null)) {
            throw ApiException.unprocessable("SELF_MODIFICATION", "لا يمكنك تعديل صلاحيات حسابك بنفسك");
        }
        if (req.role() != null) {
            u.setRole(req.role());
        }
        if (req.status() != null) {
            u.setStatus(req.status());
        }
        return UserDto.from(u);
    }

    // ---------------------------------------------------------------- statistics

    @Transactional(readOnly = true)
    public StatsDto stats(UUID restaurantId, Instant from, Instant to) {
        Instant end = to == null ? Instant.now().plus(Duration.ofDays(1)) : to;
        Instant start = from == null ? end.minus(Duration.ofDays(30)) : from;
        if (!start.isBefore(end)) {
            throw ApiException.badRequest("INVALID_RANGE", "الفترة الزمنية غير صحيحة");
        }

        Collection<UUID> scope;
        if (restaurantId != null) {
            access.requireRestaurant(restaurantId);
            scope = List.of(restaurantId);
        } else {
            scope = access.restaurantScope(); // null = admin, sees everything
        }
        boolean all = scope == null;
        if (!all && scope.isEmpty()) {
            return new StatsDto(start, end, 0, 0, 0, 0, 0, Map.of(), List.of(), List.of());
        }

        Object[] row = (all ? orderRepository.statsAll(OrderStatus.DELIVERED, start, end)
                : orderRepository.statsForRestaurants(OrderStatus.DELIVERED, start, end, scope)).get(0);
        long delivered = ((Number) row[0]).longValue();
        long revenue = ((Number) row[1]).longValue();
        long commission = ((Number) row[2]).longValue();
        long savings = ((Number) row[3]).longValue();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        for (Object[] r : all ? orderRepository.countByStatusAll(start, end)
                : orderRepository.countByStatusForRestaurants(start, end, scope)) {
            byStatus.put(((OrderStatus) r[0]).name(), ((Number) r[1]).longValue());
        }

        List<Offer> byOrders;
        List<Offer> byViews;
        if (all) {
            byOrders = offerRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "orderCount"))).getContent();
            byViews = offerRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "viewCount"))).getContent();
        } else {
            byOrders = offerRepository.findTopByRestaurants(scope, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "orderCount")));
            byViews = offerRepository.findTopByRestaurants(scope, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "viewCount")));
        }
        return new StatsDto(start, end, delivered, revenue, commission, savings,
                delivered == 0 ? 0 : revenue / delivered, byStatus,
                byOrders.stream().map(this::top).toList(), byViews.stream().map(this::top).toList());
    }

    private TopOfferDto top(Offer o) {
        return new TopOfferDto(o.getId(), o.getTitle(), o.getRestaurant().getName(), o.getOrderCount(), o.getViewCount());
    }
}
