package com.shofiniakol.discover;

import com.shofiniakol.discover.CatalogDtos.CategoryDto;
import com.shofiniakol.discover.CatalogDtos.OfferSummaryDto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public final class DiscoverDtos {

    private DiscoverDtos() {
    }

    /**
     * Body of POST /api/v1/discover. {@code category} is a slug ("burger") or "all".
     * {@code sort}: best_value | biggest_saving | top_rated | nearest | lowest_price.
     */
    public record DiscoverRequest(
            @NotNull(message = "حدد ميزانيتك") @Positive(message = "الميزانية يجب أن تكون أكبر من صفر") Long budget,
            String category, Double latitude, Double longitude, String sort,
            Integer page, Integer limit,
            Double maxDistanceKm, Double minRating, Long minPrice,
            Boolean openNow, Boolean includeDelivery) {
    }

    public record DiscoverResponse(long budget, String category, String sort, List<OfferSummaryDto> items,
                                   int page, int limit, long total, int totalPages, boolean hasNext,
                                   Long suggestedBudget) {
    }

    public record SortOptionDto(String key, String label) {
    }

    public record Range(long min, long max) {
    }

    public record FiltersResponse(List<SortOptionDto> sortOptions, List<CategoryDto> categories,
                                  List<Long> budgetPresets, List<Double> distanceOptionsKm,
                                  List<Double> ratingOptions, Range priceRange) {
    }
}
