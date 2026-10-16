package com.shofiniakol.discover;

import com.shofiniakol.catalog.Category;
import com.shofiniakol.catalog.CategoryService;
import com.shofiniakol.catalog.OfferRepository;
import com.shofiniakol.common.PageResponse;
import com.shofiniakol.config.AppProperties;
import com.shofiniakol.discover.CatalogDtos.OfferSummaryDto;
import com.shofiniakol.discover.DiscoverDtos.DiscoverRequest;
import com.shofiniakol.discover.DiscoverDtos.DiscoverResponse;
import com.shofiniakol.discover.DiscoverDtos.FiltersResponse;
import com.shofiniakol.discover.DiscoverDtos.Range;
import com.shofiniakol.discover.DiscoverDtos.SortOptionDto;
import com.shofiniakol.favorite.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscoverService {

    private final OfferRankingService ranking;
    private final CategoryService categoryService;
    private final OfferMapper mapper;
    private final FavoriteService favorites;
    private final OfferRepository offerRepository;
    private final AppProperties props;

    /** "شو فيني آكل بهيك سعر؟" - every servable offer inside the budget, ranked. */
    @Transactional(readOnly = true)
    public DiscoverResponse discover(DiscoverRequest req, UUID userId) {
        Category category = categoryService.resolve(req.category());
        SortOption sort = SortOption.parse(req.sort());

        var query = new OfferRankingService.Query(req.budget(), Boolean.TRUE.equals(req.includeDelivery()),
                category == null ? null : category.getId(), null, req.latitude(), req.longitude(),
                req.maxDistanceKm(), req.minRating(), req.minPrice(), Boolean.TRUE.equals(req.openNow()), false);
        List<ScoredOffer> ranked = ranking.find(query, sort);

        var favs = favorites.offerIds(userId);
        List<OfferSummaryDto> all = ranked.stream().map(s -> mapper.summary(s, favs)).toList();
        PageResponse<OfferSummaryDto> page = PageResponse.of(all, req.page(), req.limit());

        Long suggested = null;
        if (all.isEmpty()) {
            Long cheapest = ranking.cheapestPrice(category == null ? null : category.getId());
            if (cheapest != null && cheapest > req.budget()) {
                suggested = cheapest;
            }
        }
        return new DiscoverResponse(req.budget(), category == null ? "all" : category.getSlug(), sort.key(),
                page.items(), page.page(), page.limit(), page.total(), page.totalPages(), page.hasNext(), suggested);
    }

    @Transactional(readOnly = true)
    public FiltersResponse filters() {
        Instant now = Instant.now();
        var servable = offerRepository.findAll(OfferSpecs.servable(now, null, null, null, null, null));
        long min = servable.stream().mapToLong(o -> o.getPrice()).min().orElse(0);
        long max = servable.stream().mapToLong(o -> o.getPrice()).max().orElse(0);
        return new FiltersResponse(
                Arrays.stream(SortOption.values()).map(s -> new SortOptionDto(s.key(), s.label())).toList(),
                categoryService.active().stream().map(mapper::category).toList(),
                props.getDiscover().getBudgetPresets(),
                List.of(1.0, 3.0, 5.0, 10.0),
                List.of(3.0, 3.5, 4.0, 4.5),
                new Range(min, max));
    }
}
