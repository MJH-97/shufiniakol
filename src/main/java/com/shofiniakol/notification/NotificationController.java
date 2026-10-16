package com.shofiniakol.notification;

import com.shofiniakol.common.PageResponse;
import com.shofiniakol.notification.NotificationService.NotificationDto;
import com.shofiniakol.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public PageResponse<NotificationDto> list(@RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer limit) {
        return service.list(SecurityUtils.currentUserId(), page, limit);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", service.unreadCount(SecurityUtils.currentUserId()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        service.markRead(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    /** "تحديد الكل كمقروء" */
    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", service.markAllRead(SecurityUtils.currentUserId()));
    }
}
