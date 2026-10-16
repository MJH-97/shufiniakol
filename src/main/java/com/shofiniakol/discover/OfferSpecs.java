package com.shofiniakol.discover;

import com.shofiniakol.catalog.Offer;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class OfferSpecs {

    private OfferSpecs() {
    }

    /** Offers a customer could order right now (time-of-week windows are checked later in Java). */
    public static Specification<Offer> servable(Instant now, Long maxPrice, Long minPrice, UUID categoryId,
                                                UUID restaurantId, Instant endsBefore) {
        return (root, query, cb) -> {
            Join<Object, Object> restaurant = root.join("restaurant");
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isTrue(root.get("active")));
            p.add(cb.isTrue(restaurant.get("active")));
            p.add(cb.lessThanOrEqualTo(root.<Instant>get("startsAt"), now));
            p.add(cb.or(cb.isNull(root.get("endsAt")), cb.greaterThan(root.<Instant>get("endsAt"), now)));
            p.add(cb.or(cb.isNull(root.get("stockRemaining")), cb.greaterThan(root.<Integer>get("stockRemaining"), 0)));
            if (maxPrice != null) {
                p.add(cb.lessThanOrEqualTo(root.<Long>get("price"), maxPrice));
            }
            if (minPrice != null) {
                p.add(cb.greaterThanOrEqualTo(root.<Long>get("price"), minPrice));
            }
            if (categoryId != null) {
                p.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (restaurantId != null) {
                p.add(cb.equal(restaurant.get("id"), restaurantId));
            }
            if (endsBefore != null) {
                p.add(cb.isNotNull(root.get("endsAt")));
                p.add(cb.lessThanOrEqualTo(root.<Instant>get("endsAt"), endsBefore));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    /** Dashboard listing: optional restaurant / active filters, optionally limited to an owner's restaurants. */
    public static Specification<Offer> dashboard(UUID restaurantId, Boolean active, UUID ownerId) {
        return (root, query, cb) -> {
            Join<Object, Object> restaurant = root.join("restaurant");
            List<Predicate> p = new ArrayList<>();
            if (restaurantId != null) {
                p.add(cb.equal(restaurant.get("id"), restaurantId));
            }
            if (active != null) {
                p.add(cb.equal(root.get("active"), active));
            }
            if (ownerId != null) {
                p.add(cb.equal(restaurant.get("ownerId"), ownerId));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
