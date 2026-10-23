package com.shofiniakol.notification;

import com.shofiniakol.common.ApiException;
import com.shofiniakol.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * In-app notification inbox. Content is stored in Arabic (the product's primary language).
 * Push delivery (FCM/APNs) can be plugged in inside {@link #create}.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;

    /** Joins the caller's transaction when there is one, otherwise runs in its own. */
    @Transactional(propagation = Propagation.REQUIRED)
    public Notification create(UUID userId, NotificationType type, String title, String body,
                               UUID offerId, UUID orderId, UUID restaurantId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setOfferId(offerId);
        n.setOrderId(orderId);
        n.setRestaurantId(restaurantId);
        return repo.save(n);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(UUID userId, Integer page, Integer limit) {
        int p = PageResponse.normalizePage(page);
        int l = PageResponse.normalizeLimit(limit);
        return PageResponse.from(repo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(p - 1, l)),
                NotificationDto::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return repo.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID id) {
        Notification n = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("NOTIFICATION_NOT_FOUND", "الإشعار غير موجود"));
        if (n.getReadAt() == null) {
            n.setReadAt(Instant.now());
        }
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return repo.markAllRead(userId, Instant.now());
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Notification n = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("NOTIFICATION_NOT_FOUND", "الإشعار غير موجود"));
        repo.delete(n);
    }

    @Transactional
    public int deleteAll(UUID userId) {
        return repo.deleteByUser(userId);
    }

    public record NotificationDto(UUID id, NotificationType type, String title, String body, UUID offerId,
                                  UUID orderId, UUID restaurantId, boolean read, Instant createdAt) {
        static NotificationDto from(Notification n) {
            return new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getOfferId(),
                    n.getOrderId(), n.getRestaurantId(), n.getReadAt() != null, n.getCreatedAt());
        }
    }
}
