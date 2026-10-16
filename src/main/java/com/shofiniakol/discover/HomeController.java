package com.shofiniakol.discover;

import com.shofiniakol.discover.HomeService.HomeResponse;
import com.shofiniakol.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home")
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService service;

    @GetMapping
    public HomeResponse home(@RequestParam(required = false) Double latitude,
                             @RequestParam(required = false) Double longitude,
                             @RequestParam(required = false) Long budget) {
        return service.home(latitude, longitude, budget, SecurityUtils.currentUserIdOrNull());
    }
}
