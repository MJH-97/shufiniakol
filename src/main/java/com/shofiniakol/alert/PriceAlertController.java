package com.shofiniakol.alert;

import com.shofiniakol.alert.PriceAlertService.PriceAlertDto;
import com.shofiniakol.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Price alerts")
@RestController
@RequestMapping("/api/v1/price-alerts")
@RequiredArgsConstructor
public class PriceAlertController {

    private final PriceAlertService service;

    public record CreateAlertRequest(
            @NotNull(message = "offer_id مطلوب") UUID offerId,
            @NotNull(message = "حدد السعر المطلوب") @Positive(message = "السعر المطلوب غير صحيح") Long targetPrice) {
    }

    @GetMapping
    public List<PriceAlertDto> list() {
        return service.list(SecurityUtils.currentUserId());
    }

    /** One alert per user+offer: posting again updates the target price. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PriceAlertDto create(@Valid @RequestBody CreateAlertRequest req) {
        return service.createOrUpdate(SecurityUtils.currentUserId(), req.offerId(), req.targetPrice());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
