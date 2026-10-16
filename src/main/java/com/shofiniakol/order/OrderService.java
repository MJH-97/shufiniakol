package com.shofiniakol.order;

import com.shofiniakol.catalog.AvailabilityService;
import com.shofiniakol.catalog.DeliveryPricingService;
import com.shofiniakol.catalog.DeliveryPricingService.DeliveryQuote;
import com.shofiniakol.catalog.Offer;
import com.shofiniakol.catalog.OfferAddon;
import com.shofiniakol.catalog.OfferRepository;
import com.shofiniakol.catalog.Restaurant;
import com.shofiniakol.common.ApiException;
import com.shofiniakol.common.GeoUtils;
import com.shofiniakol.common.PageResponse;
import com.shofiniakol.notification.NotificationService;
import com.shofiniakol.notification.NotificationType;
import com.shofiniakol.order.OrderDtos.AddonLineDto;
import com.shofiniakol.order.OrderDtos.CreateOrderRequest;
import com.shofiniakol.order.OrderDtos.OrderDto;
import com.shofiniakol.order.OrderDtos.OrderItemDto;
import com.shofiniakol.order.OrderDtos.OrderLineRequest;
import com.shofiniakol.order.OrderDtos.OrderRestaurantDto;
import com.shofiniakol.order.OrderDtos.OrderSummaryDto;
import com.shofiniakol.order.OrderDtos.PaymentDto;
import com.shofiniakol.order.OrderDtos.TimelineDto;
import com.shofiniakol.review.ReviewRepository;
import com.shofiniakol.user.Address;
import com.shofiniakol.user.AddressRepository;
import com.shofiniakol.user.User;
import com.shofiniakol.user.UserRepository;
import com.shofiniakol.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OfferRepository offerRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ReviewRepository reviewRepository;
    private final DeliveryPricingService delivery;
    private final AvailabilityService availability;
    private final NotificationService notifications;

    // ------------------------------------------------------------------ create

    @Transactional
    public OrderDto create(UUID userId, CreateOrderRequest req) {
        String requestId = req.clientRequestId() == null || req.clientRequestId().isBlank()
                ? null : req.clientRequestId().trim();
        if (requestId != null) {
            var existing = orderRepository.findByUserIdAndClientRequestId(userId, requestId);
            if (existing.isPresent()) {
                return toDto(existing.get()); // idempotent retry (double tap / flaky network)
            }
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHORIZED", "يرجى تسجيل الدخول"));
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw ApiException.forbidden("ACCOUNT_BLOCKED", "تم إيقاف هذا الحساب، تواصل مع الدعم");
        }
        Instant now = Instant.now();

        List<UUID> offerIds = req.items().stream().map(OrderLineRequest::offerId).distinct().toList();
        Map<UUID, Offer> offers = offerRepository.findAllById(offerIds).stream()
                .collect(Collectors.toMap(Offer::getId, Function.identity()));
        for (UUID id : offerIds) {
            if (!offers.containsKey(id)) {
                throw ApiException.notFound("OFFER_NOT_FOUND", "أحد العروض لم يعد موجوداً");
            }
        }
        Restaurant restaurant = offers.get(offerIds.get(0)).getRestaurant();
        for (Offer o : offers.values()) {
            if (!o.getRestaurant().getId().equals(restaurant.getId())) {
                throw ApiException.unprocessable("MULTIPLE_RESTAURANTS", "لا يمكن الطلب من أكثر من مطعم في طلب واحد");
            }
        }
        if (!restaurant.isActive() || !restaurant.isAcceptingOrders()) {
            throw ApiException.unprocessable("RESTAURANT_NOT_ACCEPTING", "المطعم لا يستقبل الطلبات حالياً");
        }
        if (!availability.isRestaurantOpen(restaurant, now)) {
            throw ApiException.unprocessable("RESTAURANT_CLOSED", "المطعم مغلق حالياً");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setRestaurant(restaurant);
        order.setDeliveryType(req.deliveryType());
        order.setContactPhone(user.getPhone());
        order.setClientRequestId(requestId);
        order.setDriverNotes(blankToNull(req.driverNotes()));

        long subtotal = 0;
        long saving = 0;
        Map<UUID, Integer> quantityByOffer = new HashMap<>();
        for (OrderLineRequest line : req.items()) {
            Offer offer = offers.get(line.offerId());
            if (!availability.isOfferAvailable(offer, now)) {
                throw ApiException.unprocessable("OFFER_UNAVAILABLE", "العرض \"" + offer.getTitle() + "\" غير متاح حالياً");
            }
            int qty = line.quantity();
            List<UUID> addonIds = line.addonIds() == null ? List.of() : line.addonIds();
            if (new HashSet<>(addonIds).size() != addonIds.size()) {
                throw ApiException.badRequest("INVALID_ADDON", "إضافة مكررة");
            }
            if (addonIds.size() > offer.getMaxAddons()) {
                throw ApiException.badRequest("TOO_MANY_ADDONS",
                        "يمكنك اختيار " + offer.getMaxAddons() + " إضافات كحد أقصى لهذا العرض");
            }
            Map<UUID, OfferAddon> available = offer.getAddons().stream().filter(OfferAddon::isAvailable)
                    .collect(Collectors.toMap(OfferAddon::getId, Function.identity()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setOfferId(offer.getId());
            item.setTitle(offer.getTitle());
            item.setImageUrl(offer.getImageUrl());
            item.setUnitPrice(offer.getPrice());
            item.setOriginalUnitPrice(offer.getOriginalPrice());
            item.setQuantity(qty);
            item.setNote(blankToNull(line.note()));
            long addonsTotal = 0;
            for (UUID addonId : addonIds) {
                OfferAddon addon = available.get(addonId);
                if (addon == null) {
                    throw ApiException.badRequest("INVALID_ADDON", "إضافة غير صالحة لهذا العرض");
                }
                OrderItemAddon snap = new OrderItemAddon();
                snap.setOrderItem(item);
                snap.setName(addon.getName());
                snap.setExtraPrice(addon.getExtraPrice());
                item.getAddons().add(snap);
                addonsTotal += addon.getExtraPrice();
            }
            item.setAddonsTotal(addonsTotal);
            item.setLineTotal((offer.getPrice() + addonsTotal) * qty);
            order.getItems().add(item);

            subtotal += item.getLineTotal();
            saving += Math.max(0, offer.getOriginalPrice() - offer.getPrice()) * qty;
            quantityByOffer.merge(offer.getId(), qty, Integer::sum);
        }

        // delivery / pickup
        long fee = 0;
        long requiredMinimum = restaurant.getMinOrderAmount();
        if (req.deliveryType() == DeliveryType.DELIVERY) {
            if (req.addressId() == null) {
                throw ApiException.badRequest("ADDRESS_REQUIRED", "اختر عنوان التوصيل");
            }
            Address address = addressRepository.findByIdAndUserId(req.addressId(), userId)
                    .orElseThrow(() -> ApiException.notFound("ADDRESS_NOT_FOUND", "العنوان غير موجود"));
            Double distance = address.getLatitude() != null && address.getLongitude() != null
                    ? GeoUtils.distanceKm(address.getLatitude(), address.getLongitude(),
                    restaurant.getLatitude(), restaurant.getLongitude()) : null;
            DeliveryQuote quote = delivery.quote(restaurant, distance);
            if (!quote.deliverable()) {
                throw ApiException.unprocessable("OUT_OF_DELIVERY_AREA", "عنوانك خارج منطقة التوصيل لهذا المطعم");
            }
            fee = quote.fee();
            requiredMinimum = Math.max(requiredMinimum, quote.minOrderAmount());
            order.setEstimatedMinutes(quote.etaMinutes());
            order.setDeliveryAddressText(address.toDisplayText());
            order.setDeliveryLatitude(address.getLatitude());
            order.setDeliveryLongitude(address.getLongitude());
        } else {
            order.setEstimatedMinutes(restaurant.getAvgPrepMinutes());
        }
        if (subtotal < requiredMinimum) {
            throw ApiException.unprocessable("MIN_ORDER_NOT_MET", "الحد الأدنى للطلب " + requiredMinimum + " ل.س")
                    .with("min_order_amount", requiredMinimum);
        }

        // payment
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(req.paymentMethod());
        payment.setAmount(subtotal + fee);
        if (req.paymentMethod() == PaymentMethod.SHAM_CASH) {
            String ref = blankToNull(req.paymentReference());
            if (ref == null || ref.length() < 4) {
                throw ApiException.badRequest("PAYMENT_REFERENCE_REQUIRED", "اكتب رقم عملية التحويل (شام كاش)");
            }
            payment.setReference(ref);
            payment.setStatus(PaymentStatus.AWAITING_VERIFICATION);
        } else {
            payment.setStatus(PaymentStatus.PENDING);
        }
        order.setPayment(payment);

        // stock: atomic, all-or-nothing (the exception below rolls the whole transaction back)
        for (Map.Entry<UUID, Integer> e : quantityByOffer.entrySet()) {
            if (offerRepository.reserveStock(e.getKey(), e.getValue()) == 0) {
                Offer o = offers.get(e.getKey());
                throw ApiException.conflict("OUT_OF_STOCK", "الكمية المطلوبة من \"" + o.getTitle() + "\" لم تعد متوفرة");
            }
        }

        order.setSubtotal(subtotal);
        order.setDeliveryFee(fee);
        order.setTotal(subtotal + fee);
        order.setSavingTotal(saving);
        order.setCommissionAmount(Math.round(subtotal * restaurant.getCommissionPercent() / 100.0));
        order.setOrderNumber("SFA-" + orderRepository.nextOrderSequence());
        order.setStatus(OrderStatus.PENDING);
        addHistory(order, OrderStatus.PENDING, null, userId);

        Order saved = orderRepository.save(order);

        if (restaurant.getOwnerId() != null) {
            notifications.create(restaurant.getOwnerId(), NotificationType.NEW_ORDER,
                    "طلب جديد " + saved.getOrderNumber(),
                    "إجمالي الطلب " + saved.getTotal() + " ل.س", null, saved.getId(), restaurant.getId());
        }
        return toDto(saved);
    }

    // ------------------------------------------------------------------ read

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryDto> list(UUID userId, String tab, Integer page, Integer limit) {
        var pageable = PageRequest.of(PageResponse.normalizePage(page) - 1, PageResponse.normalizeLimit(limit),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(
                orderRepository.findByUserIdAndStatusIn(userId, OrderStatus.forTab(tab), pageable), this::toSummary);
    }

    @Transactional(readOnly = true)
    public OrderDto get(UUID userId, UUID orderId) {
        return toDto(orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "الطلب غير موجود")));
    }

    @Transactional(readOnly = true)
    public Order getEntity(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "الطلب غير موجود"));
    }

    // ------------------------------------------------------------------ status changes

    /** Customers may cancel only before the restaurant starts preparing. */
    @Transactional
    public OrderDto cancelByCustomer(UUID userId, UUID orderId, String reason) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "الطلب غير موجود"));
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw ApiException.unprocessable("CANNOT_CANCEL", "لا يمكن إلغاء الطلب بعد بدء التحضير، تواصل مع الدعم");
        }
        changeStatus(order, OrderStatus.CANCELLED, blankToNull(reason), userId);
        return toDto(order);
    }

    /** Single place where an order moves forward (used by the dashboard and by cancellation). */
    @Transactional
    public Order changeStatus(Order order, OrderStatus next, String note, UUID actorId) {
        if (!order.getStatus().canMoveTo(next, order.getDeliveryType())) {
            throw ApiException.unprocessable("INVALID_STATUS_TRANSITION",
                    "لا يمكن نقل الطلب من \"" + order.getStatus().label() + "\" إلى \"" + next.label() + "\"");
        }
        order.setStatus(next);
        addHistory(order, next, note, actorId);
        Payment payment = order.getPayment();
        Instant now = Instant.now();

        if (next == OrderStatus.DELIVERED) {
            order.setDeliveredAt(now);
            if (payment != null && payment.getMethod() == PaymentMethod.CASH_ON_DELIVERY) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(now);
            }
        } else if (next == OrderStatus.CANCELLED) {
            order.setCancelledReason(note);
            Map<UUID, Integer> qty = new HashMap<>();
            for (OrderItem i : order.getItems()) {
                if (i.getOfferId() != null) {
                    qty.merge(i.getOfferId(), i.getQuantity(), Integer::sum);
                }
            }
            qty.forEach(offerRepository::releaseStock);
            if (payment != null) {
                payment.setStatus(payment.getStatus() == PaymentStatus.PAID
                        ? PaymentStatus.REFUNDED : PaymentStatus.CANCELLED);
            }
        }
        notifyCustomer(order, next);
        return order;
    }

    @Transactional
    public void updatePaymentStatus(Order order, PaymentStatus status) {
        Payment p = order.getPayment();
        if (p == null) {
            throw ApiException.notFound("PAYMENT_NOT_FOUND", "لا توجد عملية دفع لهذا الطلب");
        }
        p.setStatus(status);
        if (status == PaymentStatus.PAID && p.getPaidAt() == null) {
            p.setPaidAt(Instant.now());
        }
    }

    // ------------------------------------------------------------------ mapping

    public OrderDto toDto(Order o) {
        Restaurant r = o.getRestaurant();
        boolean reviewed = reviewRepository.existsByOrderIdAndOfferIdIsNull(o.getId());
        Payment p = o.getPayment();
        return new OrderDto(o.getId(), o.getOrderNumber(), o.getStatus(), o.getStatus().label(), o.getStatus().tab(),
                new OrderRestaurantDto(r.getId(), r.getName(), r.getLogoUrl(), r.getPhone()),
                o.getDeliveryType(), o.getDeliveryAddressText(), o.getDeliveryLatitude(), o.getDeliveryLongitude(),
                o.getContactPhone(),
                o.getItems().stream().map(i -> new OrderItemDto(i.getOfferId(), i.getTitle(), i.getImageUrl(),
                        i.getUnitPrice(), i.getAddonsTotal(), i.getQuantity(), i.getLineTotal(), i.getNote(),
                        i.getAddons().stream().map(a -> new AddonLineDto(a.getName(), a.getExtraPrice())).toList())).toList(),
                o.getSubtotal(), o.getDeliveryFee(), o.getTotal(), o.getSavingTotal(),
                p == null ? null : new PaymentDto(p.getMethod(), p.getStatus(), p.getReference(), p.getPaidAt()),
                o.getDriverNotes(), o.getEstimatedMinutes(), o.getCancelledReason(),
                o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.CONFIRMED,
                o.getStatus() == OrderStatus.DELIVERED && !reviewed, reviewed,
                o.getHistory().stream().map(h -> new TimelineDto(h.getStatus(), h.getStatus().label(), h.getNote(),
                        h.getCreatedAt())).toList(),
                o.getCreatedAt(), o.getDeliveredAt());
    }

    public OrderSummaryDto toSummary(Order o) {
        String preview = o.getItems().stream().limit(2).map(i -> i.getQuantity() + "× " + i.getTitle())
                .collect(Collectors.joining("، "));
        if (o.getItems().size() > 2) {
            preview += " +" + (o.getItems().size() - 2);
        }
        int count = o.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        return new OrderSummaryDto(o.getId(), o.getOrderNumber(), o.getStatus(), o.getStatus().label(),
                o.getStatus().tab(), o.getRestaurant().getName(), o.getRestaurant().getLogoUrl(), count, preview,
                o.getTotal(), o.getCreatedAt());
    }

    // ------------------------------------------------------------------ helpers

    private void addHistory(Order order, OrderStatus status, String note, UUID actor) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(order);
        h.setStatus(status);
        h.setNote(note);
        h.setChangedBy(actor);
        order.getHistory().add(h);
    }

    private void notifyCustomer(Order order, OrderStatus status) {
        String restaurant = order.getRestaurant().getName();
        String num = order.getOrderNumber();
        String title;
        String body;
        switch (status) {
            case CONFIRMED -> {
                title = "تم تأكيد طلبك";
                body = restaurant + " أكّد طلبك رقم " + num;
            }
            case PREPARING -> {
                title = "طلبك قيد التحضير";
                body = "بدأ " + restaurant + " بتحضير طلبك رقم " + num;
            }
            case OUT_FOR_DELIVERY -> {
                title = "طلبك خرج للتوصيل";
                body = "طلبك رقم " + num + " بالطريق إليك";
            }
            case READY_FOR_PICKUP -> {
                title = "طلبك جاهز للاستلام";
                body = "تقدر تستلم طلبك رقم " + num + " من " + restaurant;
            }
            case DELIVERED -> {
                title = "بالهنا والعافية!";
                body = "تم تسليم طلبك رقم " + num + ". قيّم تجربتك مع " + restaurant;
            }
            case CANCELLED -> {
                title = "تم إلغاء طلبك";
                body = "تم إلغاء الطلب رقم " + num;
            }
            default -> {
                return;
            }
        }
        notifications.create(order.getUserId(), NotificationType.ORDER_STATUS, title, body, null,
                order.getId(), order.getRestaurant().getId());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
