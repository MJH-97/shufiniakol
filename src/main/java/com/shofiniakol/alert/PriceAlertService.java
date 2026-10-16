package com.shofiniakol.alert;

import com.shofiniakol.catalog.Offer;
import com.shofiniakol.catalog.OfferRepository;
import com.shofiniakol.common.ApiException;
import com.shofiniakol.notification.NotificationService;
import com.shofiniakol.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriceAlertService {

    private final PriceAlertRepository repo;
    private final OfferRepository offerRepository;
    private final NotificationService notifications;

    public record PriceAlertDto(UUID id, UUID offerId, String offerTitle, String offerImageUrl,
                                long currentPrice, long targetPrice, boolean active, boolean triggered,
                                Instant createdAt) {
    }

    @Transactional
    public PriceAlertDto createOrUpdate(UUID userId, UUID offerId, long targetPrice) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> ApiException.notFound("OFFER_NOT_FOUND", "العرض غير موجود"));
        if (targetPrice >= offer.getPrice()) {
            throw ApiException.badRequest("ALERT_TARGET_TOO_HIGH",
                    "السعر المطلوب يجب أن يكون أقل من السعر الحالي (" + offer.getPrice() + " ل.س)");
        }
        PriceAlert alert = repo.findByUserIdAndOfferId(userId, offerId).orElseGet(PriceAlert::new);
        alert.setUserId(userId);
        alert.setOfferId(offerId);
        alert.setTargetPrice(targetPrice);
        alert.setActive(true);
        alert.setTriggeredAt(null);
        return toDto(repo.save(alert), offer);
    }

    @Transactional(readOnly = true)
    public List<PriceAlertDto> list(UUID userId) {
        List<PriceAlert> alerts = repo.findByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, Offer> offers = offerRepository.findAllById(alerts.stream().map(PriceAlert::getOfferId).toList())
                .stream().collect(Collectors.toMap(Offer::getId, Function.identity()));
        return alerts.stream().filter(a -> offers.containsKey(a.getOfferId()))
                .map(a -> toDto(a, offers.get(a.getOfferId()))).toList();
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        repo.findByIdAndUserId(id, userId).ifPresent(repo::delete);
    }

    /** Call after an offer's price changed: fires matching alerts and re-arms those the price rose above. */
    @Transactional
    public void onPriceChanged(Offer offer) {
        repo.rearm(offer.getId(), offer.getPrice());
        List<PriceAlert> hits = repo.findTriggerable(offer.getId(), offer.getPrice());
        for (PriceAlert a : hits) {
            a.setTriggeredAt(Instant.now());
            String price = String.format(Locale.ENGLISH, "%,d", offer.getPrice());
            notifications.create(a.getUserId(), NotificationType.PRICE_DROP,
                    "انخفض سعر وجبة " + offer.getTitle() + "!",
                    "السعر الآن " + price + " ل.س من " + offer.getRestaurant().getName(),
                    offer.getId(), null, offer.getRestaurant().getId());
        }
    }

    private PriceAlertDto toDto(PriceAlert a, Offer o) {
        return new PriceAlertDto(a.getId(), o.getId(), o.getTitle(), o.getImageUrl(), o.getPrice(),
                a.getTargetPrice(), a.isActive(), a.getTriggeredAt() != null, a.getCreatedAt());
    }
}
