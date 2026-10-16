package com.shofiniakol.admin;

import com.shofiniakol.admin.AdminDtos.AdminRestaurantDto;
import com.shofiniakol.admin.AdminDtos.CategoryRequest;
import com.shofiniakol.admin.AdminDtos.MenuItemAdminDto;
import com.shofiniakol.admin.AdminDtos.MenuItemRequest;
import com.shofiniakol.admin.AdminDtos.RestaurantRequest;
import com.shofiniakol.admin.AdminDtos.ZoneDto;
import com.shofiniakol.admin.AdminDtos.ZoneRequest;
import com.shofiniakol.catalog.Category;
import com.shofiniakol.catalog.CategoryRepository;
import com.shofiniakol.catalog.DeliveryZone;
import com.shofiniakol.catalog.DeliveryZoneRepository;
import com.shofiniakol.catalog.MenuItem;
import com.shofiniakol.catalog.MenuItemRepository;
import com.shofiniakol.catalog.Restaurant;
import com.shofiniakol.catalog.RestaurantRepository;
import com.shofiniakol.common.ApiException;
import com.shofiniakol.common.PageResponse;
import com.shofiniakol.common.PhoneUtils;
import com.shofiniakol.discover.CategoryBenchmarkService;
import com.shofiniakol.discover.OfferMapper;
import com.shofiniakol.user.Role;
import com.shofiniakol.user.User;
import com.shofiniakol.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final DeliveryZoneRepository zoneRepository;
    private final UserRepository userRepository;
    private final AdminAccess access;
    private final OfferMapper mapper;
    private final CategoryBenchmarkService benchmarks;

    // ------------------------------------------------------------ restaurants

    @Transactional(readOnly = true)
    public PageResponse<AdminRestaurantDto> listRestaurants(String q, Integer page, Integer limit) {
        var pageable = PageRequest.of(PageResponse.normalizePage(page) - 1, PageResponse.normalizeLimit(limit),
                Sort.by("name"));
        String query = q == null ? "" : q.trim();
        var result = access.isAdmin()
                ? restaurantRepository.searchAll(query, pageable)
                : restaurantRepository.searchByOwner(access.currentUserId(), query, pageable);
        return PageResponse.from(result, this::toDto);
    }

    @Transactional(readOnly = true)
    public AdminRestaurantDto getRestaurant(UUID id) {
        return toDto(access.requireRestaurant(id));
    }

    @Transactional
    public AdminRestaurantDto createRestaurant(RestaurantRequest req) {
        Restaurant r = new Restaurant();
        apply(r, req, true);
        return toDto(restaurantRepository.save(r));
    }

    @Transactional
    public AdminRestaurantDto updateRestaurant(UUID id, RestaurantRequest req) {
        Restaurant r = access.requireRestaurant(id);
        apply(r, req, access.isAdmin());
        return toDto(r);
    }

    /** Soft delete: the restaurant and all of its offers disappear from the app but history is kept. */
    @Transactional
    public void deactivateRestaurant(UUID id) {
        restaurantRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("RESTAURANT_NOT_FOUND", "المطعم غير موجود"))
                .setActive(false);
    }

    private void apply(Restaurant r, RestaurantRequest q, boolean admin) {
        if ((q.opensAt() == null) != (q.closesAt() == null)) {
            throw ApiException.badRequest("INVALID_HOURS", "حدد وقت الفتح والإغلاق معاً");
        }
        r.setName(q.name().trim());
        r.setDescription(blank(q.description()));
        r.setLogoUrl(blank(q.logoUrl()));
        r.setCoverUrl(blank(q.coverUrl()));
        r.setPhone(blank(q.phone()));
        if (q.city() != null && !q.city().isBlank()) {
            r.setCity(q.city().trim());
        }
        r.setArea(blank(q.area()));
        r.setAddressLine(blank(q.addressLine()));
        r.setLatitude(q.latitude());
        r.setLongitude(q.longitude());
        r.setOpensAt(q.opensAt());
        r.setClosesAt(q.closesAt());
        if (q.acceptingOrders() != null) {
            r.setAcceptingOrders(q.acceptingOrders());
        }
        if (q.defaultDeliveryFee() != null) {
            r.setDefaultDeliveryFee(q.defaultDeliveryFee());
        }
        if (q.minOrderAmount() != null) {
            r.setMinOrderAmount(q.minOrderAmount());
        }
        if (q.avgPrepMinutes() != null) {
            r.setAvgPrepMinutes(q.avgPrepMinutes());
        }
        if (q.categoryIds() != null) {
            List<Category> cats = categoryRepository.findAllById(q.categoryIds());
            if (cats.size() != q.categoryIds().size()) {
                throw ApiException.badRequest("INVALID_CATEGORY", "إحدى الفئات غير موجودة");
            }
            r.getCategories().clear();
            r.getCategories().addAll(cats);
        }
        if (admin) {
            if (q.active() != null) {
                r.setActive(q.active());
            }
            if (q.commissionPercent() != null) {
                r.setCommissionPercent(q.commissionPercent());
            }
            if (q.ownerPhone() != null) {
                r.setOwnerId(q.ownerPhone().isBlank() ? null : ownerFor(q.ownerPhone()).getId());
            }
        }
    }

    /** Finds (or pre-creates) the owner account by phone and promotes customers to RESTAURANT_OWNER. */
    private User ownerFor(String rawPhone) {
        String phone = PhoneUtils.normalize(rawPhone);
        User u = userRepository.findByPhone(phone).orElseGet(() -> {
            User n = new User();
            n.setPhone(phone);
            return n;
        });
        if (u.getRole() == Role.CUSTOMER) {
            u.setRole(Role.RESTAURANT_OWNER);
        }
        return userRepository.save(u);
    }

    private AdminRestaurantDto toDto(Restaurant r) {
        String ownerPhone = r.getOwnerId() == null ? null
                : userRepository.findById(r.getOwnerId()).map(User::getPhone).orElse(null);
        return new AdminRestaurantDto(r.getId(), r.getOwnerId(), ownerPhone, r.getName(), r.getDescription(),
                r.getLogoUrl(), r.getCoverUrl(), r.getPhone(), r.getCity(), r.getArea(), r.getAddressLine(),
                r.getLatitude(), r.getLongitude(), r.getOpensAt(), r.getClosesAt(), r.isAcceptingOrders(),
                r.isActive(), r.getDefaultDeliveryFee(), r.getMinOrderAmount(), r.getAvgPrepMinutes(),
                r.getCommissionPercent(), r.getRatingAvg(), r.getRatingCount(),
                r.getCategories().stream().map(mapper::category).toList(), r.getCreatedAt());
    }

    // ------------------------------------------------------------ categories (admin only)

    @Transactional(readOnly = true)
    public List<Category> allCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional
    public Category createCategory(CategoryRequest q) {
        String slug = q.slug().trim().toLowerCase();
        if (categoryRepository.existsBySlug(slug)) {
            throw ApiException.conflict("SLUG_EXISTS", "هذا المعرّف مستخدم مسبقاً");
        }
        Category c = new Category();
        c.setSlug(slug);
        applyCategory(c, q);
        benchmarks.invalidate();
        return categoryRepository.save(c);
    }

    @Transactional
    public Category updateCategory(UUID id, CategoryRequest q) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CATEGORY_NOT_FOUND", "الفئة غير موجودة"));
        String slug = q.slug().trim().toLowerCase();
        if (!slug.equals(c.getSlug()) && categoryRepository.existsBySlug(slug)) {
            throw ApiException.conflict("SLUG_EXISTS", "هذا المعرّف مستخدم مسبقاً");
        }
        c.setSlug(slug);
        applyCategory(c, q);
        return c;
    }

    /** Categories referenced by offers cannot be removed; they are hidden instead. */
    @Transactional
    public void hideCategory(UUID id) {
        categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CATEGORY_NOT_FOUND", "الفئة غير موجودة"))
                .setActive(false);
    }

    private void applyCategory(Category c, CategoryRequest q) {
        c.setNameAr(q.nameAr().trim());
        c.setNameEn(q.nameEn().trim());
        c.setNameFr(q.nameFr().trim());
        c.setEmoji(blank(q.emoji()));
        c.setIconUrl(blank(q.iconUrl()));
        if (q.sortOrder() != null) {
            c.setSortOrder(q.sortOrder());
        }
        if (q.active() != null) {
            c.setActive(q.active());
        }
    }

    // ------------------------------------------------------------ menu items

    @Transactional(readOnly = true)
    public List<MenuItemAdminDto> listMenu(UUID restaurantId) {
        access.requireRestaurant(restaurantId);
        return menuItemRepository.findByRestaurantIdOrderBySortOrderAscNameAsc(restaurantId).stream()
                .map(this::toDto).toList();
    }

    @Transactional
    public MenuItemAdminDto createMenuItem(UUID restaurantId, MenuItemRequest q) {
        Restaurant r = access.requireRestaurant(restaurantId);
        MenuItem m = new MenuItem();
        m.setRestaurant(r);
        applyMenu(m, q);
        return toDto(menuItemRepository.save(m));
    }

    @Transactional
    public MenuItemAdminDto updateMenuItem(UUID restaurantId, UUID itemId, MenuItemRequest q) {
        access.requireRestaurant(restaurantId);
        MenuItem m = menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> ApiException.notFound("MENU_ITEM_NOT_FOUND", "الصنف غير موجود"));
        applyMenu(m, q);
        return toDto(m);
    }

    @Transactional
    public void deleteMenuItem(UUID restaurantId, UUID itemId) {
        access.requireRestaurant(restaurantId);
        menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId).ifPresent(menuItemRepository::delete);
    }

    private void applyMenu(MenuItem m, MenuItemRequest q) {
        m.setName(q.name().trim());
        m.setDescription(blank(q.description()));
        m.setPrice(q.price());
        m.setImageUrl(blank(q.imageUrl()));
        if (q.available() != null) {
            m.setAvailable(q.available());
        }
        if (q.sortOrder() != null) {
            m.setSortOrder(q.sortOrder());
        }
        m.setCategory(q.categoryId() == null ? null : categoryRepository.findById(q.categoryId())
                .orElseThrow(() -> ApiException.badRequest("INVALID_CATEGORY", "الفئة غير موجودة")));
    }

    private MenuItemAdminDto toDto(MenuItem m) {
        return new MenuItemAdminDto(m.getId(), m.getName(), m.getDescription(), m.getPrice(), m.getImageUrl(),
                m.isAvailable(), m.getSortOrder(), m.getCategory() == null ? null : m.getCategory().getId());
    }

    // ------------------------------------------------------------ delivery zones

    @Transactional(readOnly = true)
    public List<ZoneDto> listZones(UUID restaurantId) {
        access.requireRestaurant(restaurantId);
        return zoneRepository.findByRestaurantIdOrderByMaxDistanceKmAsc(restaurantId).stream().map(this::toDto).toList();
    }

    @Transactional
    public ZoneDto createZone(UUID restaurantId, ZoneRequest q) {
        Restaurant r = access.requireRestaurant(restaurantId);
        DeliveryZone z = new DeliveryZone();
        z.setRestaurant(r);
        applyZone(z, q);
        return toDto(zoneRepository.save(z));
    }

    @Transactional
    public ZoneDto updateZone(UUID restaurantId, UUID zoneId, ZoneRequest q) {
        access.requireRestaurant(restaurantId);
        DeliveryZone z = zoneRepository.findByIdAndRestaurantId(zoneId, restaurantId)
                .orElseThrow(() -> ApiException.notFound("ZONE_NOT_FOUND", "المنطقة غير موجودة"));
        applyZone(z, q);
        return toDto(z);
    }

    @Transactional
    public void deleteZone(UUID restaurantId, UUID zoneId) {
        access.requireRestaurant(restaurantId);
        zoneRepository.findByIdAndRestaurantId(zoneId, restaurantId).ifPresent(zoneRepository::delete);
    }

    private void applyZone(DeliveryZone z, ZoneRequest q) {
        z.setName(q.name().trim());
        z.setMaxDistanceKm(q.maxDistanceKm());
        z.setFee(q.fee());
        z.setMinOrderAmount(q.minOrderAmount() == null ? 0 : q.minOrderAmount());
        z.setEstimatedMinutes(q.estimatedMinutes() == null ? 30 : q.estimatedMinutes());
        if (q.active() != null) {
            z.setActive(q.active());
        }
    }

    private ZoneDto toDto(DeliveryZone z) {
        return new ZoneDto(z.getId(), z.getName(), z.getMaxDistanceKm(), z.getFee(), z.getMinOrderAmount(),
                z.getEstimatedMinutes(), z.isActive());
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
