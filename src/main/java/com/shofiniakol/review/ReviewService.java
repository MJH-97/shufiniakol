package com.shofiniakol.review;

import com.shofiniakol.catalog.OfferRepository;
import com.shofiniakol.catalog.RestaurantRepository;
import com.shofiniakol.common.ApiException;
import com.shofiniakol.common.PageResponse;
import com.shofiniakol.order.Order;
import com.shofiniakol.order.OrderItem;
import com.shofiniakol.order.OrderRepository;
import com.shofiniakol.order.OrderStatus;
import com.shofiniakol.user.User;
import com.shofiniakol.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository repo;
    private final OrderRepository orderRepository;
    private final OfferRepository offerRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public record OfferRatingRequest(
            @NotNull(message = "offer_id مطلوب") UUID offerId,
            @NotNull @Min(value = 1, message = "التقييم من 1 إلى 5") @Max(value = 5, message = "التقييم من 1 إلى 5") Integer rating) {
    }

    public record ReviewRequest(
            @NotNull(message = "قيّم المطعم") @Min(value = 1, message = "التقييم من 1 إلى 5")
            @Max(value = 5, message = "التقييم من 1 إلى 5") Integer restaurantRating,
            @Size(max = 1000, message = "التعليق طويل جداً") String comment,
            List<@Valid OfferRatingRequest> offerRatings) {
    }

    public record ReviewDto(UUID id, String userName, int rating, String comment, UUID offerId, Instant createdAt,
                            boolean hidden) {
    }

    @Transactional
    public void reviewOrder(UUID userId, UUID orderId, ReviewRequest req) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "الطلب غير موجود"));
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw ApiException.unprocessable("ORDER_NOT_DELIVERED", "يمكنك التقييم بعد استلام الطلب");
        }
        if (repo.existsByOrderIdAndOfferIdIsNull(orderId)) {
            throw ApiException.conflict("ALREADY_REVIEWED", "سبق أن قيّمت هذا الطلب");
        }
        UUID restaurantId = order.getRestaurant().getId();
        String comment = req.comment() == null || req.comment().isBlank() ? null : req.comment().trim();

        Review restaurantReview = new Review();
        restaurantReview.setUserId(userId);
        restaurantReview.setOrderId(orderId);
        restaurantReview.setRestaurantId(restaurantId);
        restaurantReview.setRating(req.restaurantRating());
        restaurantReview.setComment(comment);
        repo.save(restaurantReview);

        Set<UUID> orderedOffers = order.getItems().stream().map(OrderItem::getOfferId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> touched = new HashSet<>();
        if (req.offerRatings() != null) {
            for (OfferRatingRequest r : req.offerRatings()) {
                if (!orderedOffers.contains(r.offerId()) || !touched.add(r.offerId())) {
                    continue; // ignore offers not in this order and duplicates
                }
                Review r2 = new Review();
                r2.setUserId(userId);
                r2.setOrderId(orderId);
                r2.setRestaurantId(restaurantId);
                r2.setOfferId(r.offerId());
                r2.setRating(r.rating());
                repo.save(r2);
            }
        }
        repo.flush();
        recompute(restaurantId, touched);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> forRestaurant(UUID restaurantId, Integer page, Integer limit) {
        var p = repo.findByRestaurantIdAndOfferIdIsNullAndHiddenFalseOrderByCreatedAtDesc(restaurantId,
                PageRequest.of(PageResponse.normalizePage(page) - 1, PageResponse.normalizeLimit(limit)));
        return toPage(p);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> forOffer(UUID offerId, Integer page, Integer limit) {
        var p = repo.findByOfferIdAndHiddenFalseOrderByCreatedAtDesc(offerId,
                PageRequest.of(PageResponse.normalizePage(page) - 1, PageResponse.normalizeLimit(limit)));
        return toPage(p);
    }

    /** Dashboard moderation: hides/unhides a review and refreshes the averages. */
    @Transactional
    public void setHidden(UUID reviewId, boolean hidden) {
        Review r = repo.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("REVIEW_NOT_FOUND", "التقييم غير موجود"));
        r.setHidden(hidden);
        repo.flush();
        recompute(r.getRestaurantId(), r.getOfferId() == null ? Set.of() : Set.of(r.getOfferId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> dashboardList(UUID restaurantId, Integer page, Integer limit) {
        return toPage(repo.findByRestaurantIdOrderByCreatedAtDesc(restaurantId,
                PageRequest.of(PageResponse.normalizePage(page) - 1, PageResponse.normalizeLimit(limit))));
    }

    private void recompute(UUID restaurantId, Set<UUID> offerIds) {
        Object[] rs = repo.restaurantStats(restaurantId).get(0);
        restaurantRepository.updateRating(restaurantId, avg(rs[0]), count(rs[1]));
        for (UUID id : offerIds) {
            Object[] os = repo.offerStats(id).get(0);
            offerRepository.updateRating(id, avg(os[0]), count(os[1]));
        }
    }

    private static double avg(Object o) {
        return o == null ? 0 : Math.round(((Number) o).doubleValue() * 100.0) / 100.0;
    }

    private static int count(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    private PageResponse<ReviewDto> toPage(org.springframework.data.domain.Page<Review> page) {
        Map<UUID, User> users = userRepository.findAllById(
                        page.getContent().stream().map(Review::getUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return PageResponse.from(page, r -> new ReviewDto(r.getId(), displayName(users.get(r.getUserId())),
                r.getRating(), r.getComment(), r.getOfferId(), r.getCreatedAt(), r.isHidden()));
    }

    /** First name only, for privacy. */
    private static String displayName(User u) {
        if (u == null || u.getFullName() == null || u.getFullName().isBlank()) {
            return "عميل";
        }
        return u.getFullName().trim().split("\\s+")[0];
    }
}
