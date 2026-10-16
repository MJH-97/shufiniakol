package com.shofiniakol.order;

import com.shofiniakol.common.PageResponse;
import com.shofiniakol.order.OrderDtos.CancelRequest;
import com.shofiniakol.order.OrderDtos.CreateOrderRequest;
import com.shofiniakol.order.OrderDtos.OrderDto;
import com.shofiniakol.order.OrderDtos.OrderSummaryDto;
import com.shofiniakol.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Orders")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto create(@Valid @RequestBody CreateOrderRequest req) {
        return service.create(SecurityUtils.currentUserId(), req);
    }

    /** tab = active | completed | cancelled | all (default). */
    @GetMapping
    public PageResponse<OrderSummaryDto> list(@RequestParam(required = false, defaultValue = "all") String tab,
                                              @RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer limit) {
        return service.list(SecurityUtils.currentUserId(), tab, page, limit);
    }

    /** Includes the status timeline used by order tracking. */
    @GetMapping("/{id}")
    public OrderDto get(@PathVariable UUID id) {
        return service.get(SecurityUtils.currentUserId(), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderDto cancel(@PathVariable UUID id, @Valid @RequestBody(required = false) CancelRequest req) {
        return service.cancelByCustomer(SecurityUtils.currentUserId(), id, req == null ? null : req.reason());
    }
}
