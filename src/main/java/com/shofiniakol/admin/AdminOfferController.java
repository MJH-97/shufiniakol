package com.shofiniakol.admin;

import com.shofiniakol.admin.AdminDtos.OfferAdminDto;
import com.shofiniakol.admin.AdminDtos.OfferRequest;
import com.shofiniakol.common.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.util.UUID;

@Tag(name = "Admin - Offers")
@RestController
@RequestMapping("/api/v1/admin/offers")
@RequiredArgsConstructor
public class AdminOfferController {

    private final AdminOfferService service;

    public record ActiveBody(@NotNull Boolean active) {
    }

    @GetMapping
    public PageResponse<OfferAdminDto> list(@RequestParam(name = "restaurant_id", required = false) UUID restaurantId,
                                            @RequestParam(required = false) Boolean active,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer limit) {
        return service.list(restaurantId, active, page, limit);
    }

    @GetMapping("/{id}")
    public OfferAdminDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OfferAdminDto create(@Valid @RequestBody OfferRequest req) {
        return service.create(req);
    }

    /** Full replace. Changing the price records a price-history point and fires matching price alerts. */
    @PutMapping("/{id}")
    public OfferAdminDto update(@PathVariable UUID id, @Valid @RequestBody OfferRequest req) {
        return service.update(id, req);
    }

    /** Quick enable / disable switch. */
    @PatchMapping("/{id}/status")
    public OfferAdminDto setActive(@PathVariable UUID id, @Valid @RequestBody ActiveBody body) {
        return service.setActive(id, body.active());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
