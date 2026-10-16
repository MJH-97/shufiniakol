package com.shofiniakol.admin;

import com.shofiniakol.catalog.Restaurant;
import com.shofiniakol.catalog.RestaurantRepository;
import com.shofiniakol.common.ApiException;
import com.shofiniakol.security.AuthUser;
import com.shofiniakol.security.SecurityUtils;
import com.shofiniakol.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Row-level authorisation of the dashboard: admins see everything, owners only their restaurants. */
@Component
@RequiredArgsConstructor
public class AdminAccess {

    private final RestaurantRepository restaurantRepository;

    public boolean isAdmin() {
        return SecurityUtils.current().role() == Role.ADMIN;
    }

    public UUID currentUserId() {
        return SecurityUtils.currentUserId();
    }

    public Restaurant requireRestaurant(UUID restaurantId) {
        Restaurant r = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> ApiException.notFound("RESTAURANT_NOT_FOUND", "المطعم غير موجود"));
        assertCanManage(r);
        return r;
    }

    public void assertCanManage(Restaurant r) {
        AuthUser u = SecurityUtils.current();
        if (u.role() == Role.ADMIN) {
            return;
        }
        if (u.role() == Role.RESTAURANT_OWNER && u.id().equals(r.getOwnerId())) {
            return;
        }
        throw ApiException.forbidden("FORBIDDEN", "ليس لديك صلاحية على هذا المطعم");
    }

    /** null = unrestricted (admin). */
    public UUID ownerScope() {
        return isAdmin() ? null : SecurityUtils.currentUserId();
    }

    /** Restaurants the caller may see statistics for; null = all (admin). */
    public List<UUID> restaurantScope() {
        if (isAdmin()) {
            return null;
        }
        return restaurantRepository.findByOwnerId(SecurityUtils.currentUserId()).stream().map(Restaurant::getId).toList();
    }
}
