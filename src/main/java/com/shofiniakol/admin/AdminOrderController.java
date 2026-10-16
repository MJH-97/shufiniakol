package com.shofiniakol.admin;

import com.shofiniakol.admin.AdminDtos.PaymentUpdateRequest;
import com.shofiniakol.admin.AdminDtos.StatusUpdateRequest;
import com.shofiniakol.common.PageResponse;
import com.shofiniakol.order.Order;
import com.shofiniakol.order.OrderDtos.OrderDto;
import com.shofiniakol.order.OrderRepository;
import com.shofiniakol.order.OrderService;
import com.shofiniakol.order.OrderStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Tag(name = "Admin - Orders")
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final AdminAccess access;

    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<OrderDto> list(@RequestParam(name = "restaurant_id", required = false) UUID restaurantId,
                                       @RequestParam(required = false) OrderStatus status,
                                       @RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer limit) {
        if (restaurantId != null) {
            access.requireRestaurant(restaurantId);
        }
        UUID owner = access.ownerScope();
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
        return PageResponse.from(orderRepository.findAll(spec, pageable), orderService::toDto);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public OrderDto get(@PathVariable UUID id) {
        Order o = orderService.getEntity(id);
        access.assertCanManage(o.getRestaurant());
        return orderService.toDto(o);
    }

    /** Moves the order through its lifecycle; the customer is notified automatically. */
    @PatchMapping("/{id}/status")
    @Transactional
    public OrderDto updateStatus(@PathVariable UUID id, @Valid @RequestBody StatusUpdateRequest req) {
        Order o = orderService.getEntity(id);
        access.assertCanManage(o.getRestaurant());
        orderService.changeStatus(o, req.status(), req.note(), access.currentUserId());
        return orderService.toDto(o);
    }

    /** Verify (PAID) or reject (FAILED) a Sham Cash transfer. */
    @PatchMapping("/{id}/payment")
    @Transactional
    public OrderDto updatePayment(@PathVariable UUID id, @Valid @RequestBody PaymentUpdateRequest req) {
        Order o = orderService.getEntity(id);
        access.assertCanManage(o.getRestaurant());
        orderService.updatePaymentStatus(o, req.status());
        return orderService.toDto(o);
    }
}
