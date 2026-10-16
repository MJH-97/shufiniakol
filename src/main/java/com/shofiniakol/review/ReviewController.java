package com.shofiniakol.review;

import com.shofiniakol.common.PageResponse;
import com.shofiniakol.review.ReviewService.ReviewDto;
import com.shofiniakol.review.ReviewService.ReviewRequest;
import com.shofiniakol.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Reviews")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService service;

    /** Rate the restaurant (required) and optionally individual meals, after the order was delivered. */
    @PostMapping("/api/v1/orders/{orderId}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public void review(@PathVariable UUID orderId, @Valid @RequestBody ReviewRequest req) {
        service.reviewOrder(SecurityUtils.currentUserId(), orderId, req);
    }

    @GetMapping("/api/v1/restaurants/{id}/reviews")
    public PageResponse<ReviewDto> restaurantReviews(@PathVariable UUID id,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer limit) {
        return service.forRestaurant(id, page, limit);
    }

    @GetMapping("/api/v1/offers/{id}/reviews")
    public PageResponse<ReviewDto> offerReviews(@PathVariable UUID id,
                                                @RequestParam(required = false) Integer page,
                                                @RequestParam(required = false) Integer limit) {
        return service.forOffer(id, page, limit);
    }
}
