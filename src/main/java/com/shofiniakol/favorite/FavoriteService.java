package com.shofiniakol.favorite;

import com.shofiniakol.catalog.OfferRepository;
import com.shofiniakol.catalog.RestaurantRepository;
import com.shofiniakol.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository repo;
    private final OfferRepository offerRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional(readOnly = true)
    public Set<UUID> offerIds(UUID userIdOrNull) {
        return userIdOrNull == null ? Set.of() : new HashSet<>(repo.findOfferIdsByUser(userIdOrNull));
    }

    @Transactional(readOnly = true)
    public Set<UUID> restaurantIds(UUID userIdOrNull) {
        return userIdOrNull == null ? Set.of() : new HashSet<>(repo.findRestaurantIdsByUser(userIdOrNull));
    }

    /** Idempotent: saving something already saved returns the existing favourite. */
    @Transactional
    public Favorite add(UUID userId, UUID offerId, UUID restaurantId) {
        if ((offerId == null) == (restaurantId == null)) {
            throw ApiException.badRequest("INVALID_FAVORITE", "أرسل offer_id أو restaurant_id (واحد فقط)");
        }
        if (offerId != null) {
            if (!offerRepository.existsById(offerId)) {
                throw ApiException.notFound("OFFER_NOT_FOUND", "العرض غير موجود");
            }
            Optional<Favorite> existing = repo.findByUserIdAndOfferId(userId, offerId);
            if (existing.isPresent()) {
                return existing.get();
            }
        } else {
            if (!restaurantRepository.existsById(restaurantId)) {
                throw ApiException.notFound("RESTAURANT_NOT_FOUND", "المطعم غير موجود");
            }
            Optional<Favorite> existing = repo.findByUserIdAndRestaurantId(userId, restaurantId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        Favorite f = new Favorite();
        f.setUserId(userId);
        f.setOfferId(offerId);
        f.setRestaurantId(restaurantId);
        return repo.save(f);
    }

    /** {@code id} may be the favourite's own id, or the id of the saved offer / restaurant. */
    @Transactional
    public void remove(UUID userId, UUID id) {
        Optional<Favorite> f = repo.findByIdAndUserId(id, userId);
        if (f.isEmpty()) {
            f = repo.findByUserIdAndOfferId(userId, id);
        }
        if (f.isEmpty()) {
            f = repo.findByUserIdAndRestaurantId(userId, id);
        }
        f.ifPresent(repo::delete);
    }
}
