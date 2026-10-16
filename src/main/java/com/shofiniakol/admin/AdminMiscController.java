package com.shofiniakol.admin;

import com.shofiniakol.admin.AdminDtos.StatsDto;
import com.shofiniakol.admin.AdminDtos.UserUpdateRequest;
import com.shofiniakol.common.PageResponse;
import com.shofiniakol.review.ReviewService;
import com.shofiniakol.review.ReviewService.ReviewDto;
import com.shofiniakol.user.Role;
import com.shofiniakol.user.UserDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "Admin - Users, Reviews, Stats")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMiscController {

    private final AdminMiscService service;
    private final ReviewService reviewService;
    private final AdminAccess access;

    // ---- users (admin only)
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserDto> users(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) Role role,
                                       @RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer limit) {
        return service.users(q, role, page, limit);
    }

    @PatchMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUser(@PathVariable UUID id, @RequestBody UserUpdateRequest req) {
        return service.updateUser(id, req);
    }

    // ---- reviews moderation
    @GetMapping("/restaurants/{rid}/reviews")
    public PageResponse<ReviewDto> reviews(@PathVariable UUID rid,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer limit) {
        access.requireRestaurant(rid);
        return reviewService.dashboardList(rid, page, limit);
    }

    @PostMapping("/reviews/{id}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hide(@PathVariable UUID id) {
        reviewService.setHidden(id, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reviews/{id}/unhide")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unhide(@PathVariable UUID id) {
        reviewService.setHidden(id, false);
        return ResponseEntity.noContent().build();
    }

    // ---- statistics
    /** from/to are ISO instants, e.g. 2026-10-01T00:00:00Z. Defaults to the last 30 days. */
    @GetMapping("/stats/overview")
    public StatsDto stats(@RequestParam(name = "restaurant_id", required = false) UUID restaurantId,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return service.stats(restaurantId, from, to);
    }
}
