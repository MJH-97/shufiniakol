package com.shofiniakol.discover;

import com.shofiniakol.common.PageResponse;
import com.shofiniakol.discover.CatalogDtos.RestaurantCardDto;
import com.shofiniakol.discover.CatalogDtos.RestaurantDetailDto;
import com.shofiniakol.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Restaurants")
@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService service;

    /** Map / list of nearby restaurants, each with its best offer inside the (optional) budget. */
    @GetMapping("/nearby")
    public PageResponse<RestaurantCardDto> nearby(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(name = "radius_km", required = false) Double radiusKm,
            @RequestParam(required = false) Long budget,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        return service.nearby(latitude, longitude, radiusKm, budget, category, page, limit,
                SecurityUtils.currentUserIdOrNull());
    }

    @GetMapping("/{id}")
    public RestaurantDetailDto detail(@PathVariable UUID id,
                                      @RequestParam(required = false) Double latitude,
                                      @RequestParam(required = false) Double longitude) {
        return service.detail(id, latitude, longitude, SecurityUtils.currentUserIdOrNull());
    }
}
