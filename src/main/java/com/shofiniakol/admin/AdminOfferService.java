package com.shofiniakol.admin;

import com.shofiniakol.admin.AdminDtos.AvailabilityRequest;
import com.shofiniakol.admin.AdminDtos.OfferAddonAdminDto;
import com.shofiniakol.admin.AdminDtos.OfferAddonRequest;
import com.shofiniakol.admin.AdminDtos.OfferAdminDto;
import com.shofiniakol.admin.AdminDtos.OfferItemAdminDto;
import com.shofiniakol.admin.AdminDtos.OfferItemRequest;
import com.shofiniakol.admin.AdminDtos.OfferRequest;
import com.shofiniakol.alert.PriceAlertService;
import com.shofiniakol.catalog.Category;
import com.shofiniakol.catalog.CategoryRepository;
import com.shofiniakol.catalog.Offer;
import com.shofiniakol.catalog.OfferAddon;
import com.shofiniakol.catalog.OfferAvailability;
import com.shofiniakol.catalog.OfferItem;
import com.shofiniakol.catalog.OfferRepository;
import com.shofiniakol.catalog.PriceHistory;
import com.shofiniakol.catalog.PriceHistoryRepository;
import com.shofiniakol.catalog.Restaurant;
import com.shofiniakol.common.ApiException;
import com.shofiniakol.common.Money;
import com.shofiniakol.common.PageResponse;
import com.shofiniakol.discover.CategoryBenchmarkService;
import com.shofiniakol.discover.OfferSpecs;
import com.shofiniakol.favorite.FavoriteRepository;
import com.shofiniakol.notification.NotificationService;
import com.shofiniakol.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminOfferService {

    private final OfferRepository offerRepository;
    private final CategoryRepository categoryRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PriceAlertService alertService;
    private final FavoriteRepository favoriteRepository;
    private final NotificationService notifications;
    private final CategoryBenchmarkService benchmarks;
    private final AdminAccess access;

    @Transactional(readOnly = true)
    public PageResponse<OfferAdminDto> list(UUID restaurantId, Boolean active, Integer page, Integer limit) {
        if (restaurantId != null) {
            access.requireRestaurant(restaurantId);
        }
        var pageable = PageRequest.of(PageResponse.normalizePage(page) - 1, PageResponse.normalizeLimit(limit),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(
                offerRepository.findAll(OfferSpecs.dashboard(restaurantId, active, access.ownerScope()), pageable),
                this::toDto);
    }

    @Transactional(readOnly = true)
    public OfferAdminDto get(UUID id) {
        return toDto(load(id));
    }

    @Transactional
    public OfferAdminDto create(OfferRequest req) {
        Restaurant restaurant = access.requireRestaurant(req.restaurantId());
        Offer o = new Offer();
        o.setRestaurant(restaurant);
        o.setStartsAt(Instant.now());
        apply(o, req);
        Offer saved = offerRepository.save(o);
        recordPrice(saved);
        if (saved.isActive() && restaurant.isActive()) {
            for (UUID userId : favoriteRepository.findUserIdsByRestaurant(restaurant.getId())) {
                notifications.create(userId, NotificationType.NEW_OFFER, "عرض جديد من " + restaurant.getName(),
                        saved.getTitle() + " - لمدة محدودة، لا تفوّته.", saved.getId(), null, restaurant.getId());
            }
        }
        benchmarks.invalidate();
        return toDto(saved);
    }

    @Transactional
    public OfferAdminDto update(UUID id, OfferRequest req) {
        Offer o = load(id);
        long oldPrice = o.getPrice();
        long oldOriginal = o.getOriginalPrice();
        if (!o.getRestaurant().getId().equals(req.restaurantId())) {
            o.setRestaurant(access.requireRestaurant(req.restaurantId()));
        }
        apply(o, req);
        offerRepository.flush();
        if (o.getPrice() != oldPrice || o.getOriginalPrice() != oldOriginal) {
            recordPrice(o);
            alertService.onPriceChanged(o);
        }
        benchmarks.invalidate();
        return toDto(o);
    }

    @Transactional
    public OfferAdminDto setActive(UUID id, boolean active) {
        Offer o = load(id);
        o.setActive(active);
        benchmarks.invalidate();
        return toDto(o);
    }

    /** Soft delete so that past orders, reviews and statistics keep their reference. */
    @Transactional
    public void deactivate(UUID id) {
        setActive(id, false);
    }

    private Offer load(UUID id) {
        Offer o = offerRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("OFFER_NOT_FOUND", "العرض غير موجود"));
        access.assertCanManage(o.getRestaurant());
        return o;
    }

    private void recordPrice(Offer o) {
        PriceHistory h = new PriceHistory();
        h.setOfferId(o.getId());
        h.setPrice(o.getPrice());
        h.setOriginalPrice(o.getOriginalPrice());
        priceHistoryRepository.save(h);
    }

    private void apply(Offer o, OfferRequest q) {
        if (q.originalPrice() < q.price()) {
            throw ApiException.badRequest("INVALID_PRICES", "السعر الأصلي يجب ألا يقل عن سعر العرض");
        }
        Instant starts = q.startsAt() != null ? q.startsAt() : o.getStartsAt();
        if (q.endsAt() != null && starts != null && !q.endsAt().isAfter(starts)) {
            throw ApiException.badRequest("INVALID_DATES", "تاريخ الانتهاء يجب أن يكون بعد تاريخ البدء");
        }
        Category category = categoryRepository.findById(q.categoryId())
                .orElseThrow(() -> ApiException.badRequest("INVALID_CATEGORY", "الفئة غير موجودة"));

        o.setCategory(category);
        o.setTitle(q.title().trim());
        o.setDescription(blank(q.description()));
        o.setImageUrl(blank(q.imageUrl()));
        o.getImages().clear();
        if (q.images() != null) {
            q.images().stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
                    .forEach(o.getImages()::add);
        }
        o.setPrice(q.price());
        o.setOriginalPrice(q.originalPrice());
        o.setServings(q.servings() == null ? 1 : q.servings());
        if (Boolean.TRUE.equals(q.unlimitedStock())) {
            o.setStockRemaining(null);
        } else if (q.stockRemaining() != null) {
            o.setStockRemaining(q.stockRemaining());
        }
        if (q.startsAt() != null) {
            o.setStartsAt(q.startsAt());
        }
        if (!Objects.equals(o.getEndsAt(), q.endsAt())) {
            o.setExpiryNotified(false);
        }
        o.setEndsAt(q.endsAt());
        if (q.active() != null) {
            o.setActive(q.active());
        }
        if (q.sponsored() != null && access.isAdmin()) {
            o.setSponsored(q.sponsored()); // paid promotion is an admin decision
        }

        o.getItems().clear();
        int count = 0;
        int idx = 0;
        if (q.items() != null) {
            for (OfferItemRequest i : q.items()) {
                OfferItem item = new OfferItem();
                item.setOffer(o);
                item.setName(i.name().trim());
                item.setQuantity(i.quantity() == null ? 1 : i.quantity());
                item.setMenuItemId(i.menuItemId());
                item.setSortOrder(idx++);
                o.getItems().add(item);
                count += item.getQuantity();
            }
        }
        o.setItemsCount(Math.max(1, count));

        o.getAddons().clear();
        idx = 0;
        if (q.addons() != null) {
            for (OfferAddonRequest a : q.addons()) {
                OfferAddon addon = new OfferAddon();
                addon.setOffer(o);
                addon.setName(a.name().trim());
                addon.setExtraPrice(a.extraPrice() == null ? 0 : a.extraPrice());
                addon.setAvailable(a.available() == null || a.available());
                addon.setSortOrder(idx++);
                o.getAddons().add(addon);
            }
        }
        int maxAddons = q.maxAddons() == null ? 0 : q.maxAddons();
        o.setMaxAddons(o.getAddons().isEmpty() ? 0 : Math.min(maxAddons, o.getAddons().size()));

        o.getAvailability().clear();
        if (q.availability() != null) {
            for (AvailabilityRequest a : q.availability()) {
                OfferAvailability w = new OfferAvailability();
                w.setOffer(o);
                w.setDayOfWeek(a.dayOfWeek());
                w.setStartTime(a.startTime());
                w.setEndTime(a.endTime());
                o.getAvailability().add(w);
            }
        }
    }

    private OfferAdminDto toDto(Offer o) {
        return new OfferAdminDto(o.getId(), o.getRestaurant().getId(), o.getRestaurant().getName(),
                o.getCategory().getId(), o.getTitle(), o.getDescription(), o.getImageUrl(), List.copyOf(o.getImages()),
                o.getPrice(), o.getOriginalPrice(), Money.discountPercent(o.getPrice(), o.getOriginalPrice()),
                o.getItemsCount(), o.getServings(), o.getMaxAddons(), o.getStockRemaining(), o.getStartsAt(),
                o.getEndsAt(), o.isActive(), o.isSponsored(), o.getRatingAvg(), o.getRatingCount(),
                o.getViewCount(), o.getOrderCount(),
                o.getItems().stream().map(i -> new OfferItemAdminDto(i.getId(), i.getName(), i.getQuantity(),
                        i.getMenuItemId())).toList(),
                o.getAddons().stream().map(a -> new OfferAddonAdminDto(a.getId(), a.getName(), a.getExtraPrice(),
                        a.isAvailable())).toList(),
                o.getAvailability().stream().map(a -> new AvailabilityRequest(a.getDayOfWeek(), a.getStartTime(),
                        a.getEndTime())).toList(),
                o.getCreatedAt());
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
