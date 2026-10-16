package com.shofiniakol.discover;

import com.shofiniakol.common.PageResponse;
import com.shofiniakol.discover.CatalogDtos.OfferDetailDto;
import com.shofiniakol.discover.CatalogDtos.OfferSummaryDto;
import com.shofiniakol.discover.CatalogDtos.PricePointDto;
import com.shofiniakol.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Offers")
@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService service;

    /** Today's offers / limited-time offers (filter=today|limited|all). */
    @GetMapping
    public PageResponse<OfferSummaryDto> list(
            @RequestParam(required = false, defaultValue = "today") String filter,
            @RequestParam(required = false) String category,
            @RequestParam(name = "restaurant_id", required = false) UUID restaurantId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(name = "min_price", required = false) Long minPrice,
            @RequestParam(name = "max_price", required = false) Long maxPrice,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        return service.list(filter, category, restaurantId, sort, latitude, longitude, minPrice, maxPrice,
                page, limit, SecurityUtils.currentUserIdOrNull());
    }

    @GetMapping("/{id}")
    public OfferDetailDto detail(@PathVariable UUID id,
                                 @RequestParam(required = false) Long budget,
                                 @RequestParam(required = false) Double latitude,
                                 @RequestParam(required = false) Double longitude) {
        return service.detail(id, budget, latitude, longitude, SecurityUtils.currentUserIdOrNull());
    }

    @GetMapping("/{id}/price-history")
    public List<PricePointDto> priceHistory(@PathVariable UUID id,
                                            @RequestParam(required = false, defaultValue = "30") int days) {
        return service.priceHistory(id, days);
    }
}
