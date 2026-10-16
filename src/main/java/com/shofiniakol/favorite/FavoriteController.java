package com.shofiniakol.favorite;

import com.shofiniakol.favorite.FavoriteListService.FavoritesResponse;
import com.shofiniakol.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "Favorites")
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final FavoriteListService listService;

    /** Send exactly one of offer_id / restaurant_id. */
    public record AddFavoriteRequest(UUID offerId, UUID restaurantId) {
    }

    public record FavoriteDto(UUID id, UUID offerId, UUID restaurantId, Instant createdAt) {
    }

    @GetMapping
    public FavoritesResponse list(@RequestParam(required = false) Double latitude,
                                  @RequestParam(required = false) Double longitude) {
        return listService.list(SecurityUtils.currentUserId(), latitude, longitude);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteDto add(@RequestBody AddFavoriteRequest req) {
        Favorite f = favoriteService.add(SecurityUtils.currentUserId(), req.offerId(), req.restaurantId());
        return new FavoriteDto(f.getId(), f.getOfferId(), f.getRestaurantId(), f.getCreatedAt());
    }

    /** {id} can be the favourite id or the offer / restaurant id itself. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        favoriteService.remove(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
