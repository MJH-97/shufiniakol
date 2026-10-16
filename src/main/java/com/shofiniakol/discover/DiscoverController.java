package com.shofiniakol.discover;

import com.shofiniakol.discover.DiscoverDtos.DiscoverRequest;
import com.shofiniakol.discover.DiscoverDtos.DiscoverResponse;
import com.shofiniakol.discover.DiscoverDtos.FiltersResponse;
import com.shofiniakol.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Discover")
@RestController
@RequestMapping("/api/v1/discover")
@RequiredArgsConstructor
public class DiscoverController {

    private final DiscoverService service;

    @PostMapping
    public DiscoverResponse discover(@Valid @RequestBody DiscoverRequest req) {
        return service.discover(req, SecurityUtils.currentUserIdOrNull());
    }

    /** Data for the filter sheet: sort chips, categories, budget presets, distance/rating options. */
    @GetMapping("/filters")
    public FiltersResponse filters() {
        return service.filters();
    }
}
