package com.shofiniakol.admin;

import com.shofiniakol.admin.AdminDtos.AdminRestaurantDto;
import com.shofiniakol.admin.AdminDtos.CategoryRequest;
import com.shofiniakol.admin.AdminDtos.MenuItemAdminDto;
import com.shofiniakol.admin.AdminDtos.MenuItemRequest;
import com.shofiniakol.admin.AdminDtos.RestaurantPatchRequest;
import com.shofiniakol.admin.AdminDtos.RestaurantRequest;
import com.shofiniakol.admin.AdminDtos.ZoneDto;
import com.shofiniakol.admin.AdminDtos.ZoneRequest;
import com.shofiniakol.common.PageResponse;
import com.shofiniakol.discover.CatalogDtos.CategoryDto;
import com.shofiniakol.discover.OfferMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin - Catalog")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final AdminCatalogService service;
    private final OfferMapper mapper;

    // ---- restaurants (owners: own only; create/delete: admin only)
    @GetMapping("/restaurants")
    public PageResponse<AdminRestaurantDto> restaurants(@RequestParam(required = false) String q,
                                                        @RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer limit) {
        return service.listRestaurants(q, page, limit);
    }

    @GetMapping("/restaurants/{id}")
    public AdminRestaurantDto restaurant(@PathVariable UUID id) {
        return service.getRestaurant(id);
    }

    @PostMapping("/restaurants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AdminRestaurantDto createRestaurant(@Valid @RequestBody RestaurantRequest req) {
        return service.createRestaurant(req);
    }

    @PutMapping("/restaurants/{id}")
    public AdminRestaurantDto updateRestaurant(@PathVariable UUID id, @Valid @RequestBody RestaurantRequest req) {
        return service.updateRestaurant(id, req);
    }

    /** Settings screen: partial update, so fields the form does not show (logo, hours, ...) are never wiped. */
    @PatchMapping("/restaurants/{id}")
    public AdminRestaurantDto patchRestaurant(@PathVariable UUID id, @Valid @RequestBody RestaurantPatchRequest req) {
        return service.patchRestaurant(id, req);
    }

    @DeleteMapping("/restaurants/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateRestaurant(@PathVariable UUID id) {
        service.deactivateRestaurant(id);
        return ResponseEntity.noContent().build();
    }

    // ---- categories
    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return service.allCategories().stream().map(mapper::category).toList();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryDto createCategory(@Valid @RequestBody CategoryRequest req) {
        return mapper.category(service.createCategory(req));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryDto updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest req) {
        return mapper.category(service.updateCategory(id, req));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hideCategory(@PathVariable UUID id) {
        service.hideCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ---- menu items
    @GetMapping("/restaurants/{rid}/menu-items")
    public List<MenuItemAdminDto> menu(@PathVariable UUID rid) {
        return service.listMenu(rid);
    }

    @PostMapping("/restaurants/{rid}/menu-items")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemAdminDto createMenuItem(@PathVariable UUID rid, @Valid @RequestBody MenuItemRequest req) {
        return service.createMenuItem(rid, req);
    }

    @PutMapping("/restaurants/{rid}/menu-items/{id}")
    public MenuItemAdminDto updateMenuItem(@PathVariable UUID rid, @PathVariable UUID id,
                                           @Valid @RequestBody MenuItemRequest req) {
        return service.updateMenuItem(rid, id, req);
    }

    @DeleteMapping("/restaurants/{rid}/menu-items/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable UUID rid, @PathVariable UUID id) {
        service.deleteMenuItem(rid, id);
        return ResponseEntity.noContent().build();
    }

    // ---- delivery zones & fees
    @GetMapping("/restaurants/{rid}/delivery-zones")
    public List<ZoneDto> zones(@PathVariable UUID rid) {
        return service.listZones(rid);
    }

    @PostMapping("/restaurants/{rid}/delivery-zones")
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneDto createZone(@PathVariable UUID rid, @Valid @RequestBody ZoneRequest req) {
        return service.createZone(rid, req);
    }

    @PutMapping("/restaurants/{rid}/delivery-zones/{id}")
    public ZoneDto updateZone(@PathVariable UUID rid, @PathVariable UUID id, @Valid @RequestBody ZoneRequest req) {
        return service.updateZone(rid, id, req);
    }

    @DeleteMapping("/restaurants/{rid}/delivery-zones/{id}")
    public ResponseEntity<Void> deleteZone(@PathVariable UUID rid, @PathVariable UUID id) {
        service.deleteZone(rid, id);
        return ResponseEntity.noContent().build();
    }
}
