package com.shofiniakol.user;

import com.shofiniakol.alert.PriceAlertRepository;
import com.shofiniakol.auth.RefreshTokenRepository;
import com.shofiniakol.common.ApiException;
import com.shofiniakol.favorite.FavoriteRepository;
import com.shofiniakol.notification.NotificationRepository;
import com.shofiniakol.order.OrderRepository;
import com.shofiniakol.order.OrderStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final FavoriteRepository favoriteRepository;
    private final PriceAlertRepository alertRepository;
    private final NotificationRepository notificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OrderRepository orderRepository;

    public record UpdateProfileRequest(
            @Size(max = 120, message = "الاسم طويل جداً") String fullName,
            @Email(message = "البريد الإلكتروني غير صحيح") @Size(max = 160) String email,
            @Pattern(regexp = "ar|en|fr", message = "اللغة غير مدعومة") String language) {
    }

    @Transactional(readOnly = true)
    public UserDto get(UUID userId) {
        return UserDto.from(find(userId));
    }

    @Transactional
    public UserDto update(UUID userId, UpdateProfileRequest req) {
        User u = find(userId);
        if (req.fullName() != null) {
            u.setFullName(req.fullName().isBlank() ? null : req.fullName().trim());
        }
        if (req.email() != null) {
            u.setEmail(req.email().isBlank() ? null : req.email().trim());
        }
        if (req.language() != null) {
            u.setLanguage(req.language());
        }
        return UserDto.from(u);
    }

    /**
     * In-app account deletion (required by Google Play / App Store). Personal data is removed and the
     * user row is anonymised; past orders and reviews are kept for the restaurants' accounting.
     */
    @Transactional
    public void deleteAccount(UUID userId) {
        User u = find(userId);
        if (orderRepository.existsByUserIdAndStatusIn(userId, OrderStatus.forTab("active"))) {
            throw ApiException.unprocessable("ACTIVE_ORDERS", "لا يمكن حذف الحساب لوجود طلبات قيد التنفيذ");
        }
        addressRepository.deleteByUser(userId);
        favoriteRepository.deleteByUser(userId);
        alertRepository.deleteByUser(userId);
        notificationRepository.deleteByUser(userId);
        refreshTokenRepository.deleteByUser(userId);
        u.setPhone("del-" + userId.toString().replace("-", "").substring(0, 14));
        u.setFullName(null);
        u.setEmail(null);
        u.setStatus(UserStatus.BLOCKED);
    }

    private User find(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "المستخدم غير موجود"));
    }
}
