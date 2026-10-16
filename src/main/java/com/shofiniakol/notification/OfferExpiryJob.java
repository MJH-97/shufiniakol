package com.shofiniakol.notification;

import com.shofiniakol.catalog.Offer;
import com.shofiniakol.catalog.OfferRepository;
import com.shofiniakol.config.AppProperties;
import com.shofiniakol.favorite.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Tells users who saved an offer that it is about to end. Runs on every instance; if you scale to several
 * replicas add ShedLock (or run it in one instance only) to avoid duplicate checks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfferExpiryJob {

    private final OfferRepository offerRepository;
    private final FavoriteRepository favoriteRepository;
    private final NotificationService notifications;
    private final AppProperties props;

    @Scheduled(fixedDelayString = "${app.jobs.expiry-check-ms:300000}", initialDelay = 60000)
    @Transactional
    public void notifyExpiringOffers() {
        Instant now = Instant.now();
        Instant until = now.plus(Duration.ofHours(props.getJobs().getExpiryWindowHours()));
        List<Offer> expiring = offerRepository.findExpiringSoon(now, until);
        for (Offer o : expiring) {
            List<UUID> users = favoriteRepository.findUserIdsByOffer(o.getId());
            for (UUID userId : users) {
                notifications.create(userId, NotificationType.OFFER_EXPIRING,
                        "عرض على وشك الانتهاء",
                        "عرض \"" + o.getTitle() + "\" من " + o.getRestaurant().getName() + " بينتهي قريباً",
                        o.getId(), null, o.getRestaurant().getId());
            }
            o.setExpiryNotified(true);
        }
        if (!expiring.isEmpty()) {
            log.info("Sent expiry reminders for {} offers", expiring.size());
        }
    }
}
