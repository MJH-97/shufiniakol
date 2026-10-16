package com.shofiniakol.discover;

import com.shofiniakol.catalog.Category;
import com.shofiniakol.catalog.Offer;
import com.shofiniakol.catalog.Restaurant;
import com.shofiniakol.common.GeoUtils;
import com.shofiniakol.common.Money;
import com.shofiniakol.discover.CatalogDtos.CategoryDto;
import com.shofiniakol.discover.CatalogDtos.OfferSummaryDto;
import com.shofiniakol.discover.CatalogDtos.RestaurantRefDto;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class OfferMapper {

    public CategoryDto category(Category c) {
        return new CategoryDto(c.getId(), c.getSlug(), c.localizedName(), c.getEmoji(), c.getIconUrl());
    }

    public RestaurantRefDto restaurantRef(Restaurant r, boolean open) {
        return new RestaurantRefDto(r.getId(), r.getName(), r.getLogoUrl(), r.getArea(),
                GeoUtils.round1(r.getRatingAvg()), r.getRatingCount(), open);
    }

    public OfferSummaryDto summary(ScoredOffer s, Set<UUID> favoriteOfferIds) {
        Offer o = s.offer();
        Restaurant r = o.getRestaurant();
        long price = o.getPrice();
        return new OfferSummaryDto(
                o.getId(), o.getTitle(), o.getDescription(), o.getImageUrl(),
                price, o.getOriginalPrice(),
                Money.discountPercent(price, o.getOriginalPrice()), Money.saving(price, o.getOriginalPrice()),
                GeoUtils.round1(s.effectiveRating()), displayRatingCount(o, r),
                s.distanceKm() == null ? null : GeoUtils.round1(s.distanceKm()),
                s.quote().fee(), s.quote().fee() == 0, s.quote().deliverable(), s.quote().etaMinutes(),
                s.value().score(), ValueText.label(s.value().score()),
                o.getItemsCount(), o.getEndsAt(), s.limitedTime(), o.isSponsored(),
                favoriteOfferIds.contains(o.getId()), s.available(),
                restaurantRef(r, s.restaurantOpen()), category(o.getCategory()));
    }

    /** An offer without reviews of its own shows its restaurant's review count. */
    public static int displayRatingCount(Offer o, Restaurant r) {
        return o.getRatingCount() > 0 ? o.getRatingCount() : r.getRatingCount();
    }
}
