package com.shofiniakol.catalog;

import com.shofiniakol.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryPricingService {

    private final AppProperties props;

    public record DeliveryQuote(long fee, int etaMinutes, boolean deliverable, long minOrderAmount) {
    }

    /**
     * @param distanceKm customer distance from the restaurant, or null when unknown. Unknown distance
     *                   quotes the nearest tier ("from" price) so browsing still shows a fee.
     */
    public DeliveryQuote quote(Restaurant r, Double distanceKm) {
        List<DeliveryZone> zones = r.getDeliveryZones().stream()
                .filter(DeliveryZone::isActive)
                .sorted(Comparator.comparingDouble(DeliveryZone::getMaxDistanceKm))
                .toList();
        int fallbackEta = r.getAvgPrepMinutes() + props.getDelivery().getDefaultEtaExtraMinutes();

        if (zones.isEmpty()) {
            boolean ok = distanceKm == null || distanceKm <= props.getDelivery().getDefaultMaxDistanceKm();
            return new DeliveryQuote(r.getDefaultDeliveryFee(), fallbackEta, ok, r.getMinOrderAmount());
        }
        if (distanceKm == null) {
            DeliveryZone z = zones.get(0);
            return new DeliveryQuote(z.getFee(), z.getEstimatedMinutes(), true,
                    Math.max(r.getMinOrderAmount(), z.getMinOrderAmount()));
        }
        for (DeliveryZone z : zones) {
            if (distanceKm <= z.getMaxDistanceKm()) {
                return new DeliveryQuote(z.getFee(), z.getEstimatedMinutes(), true,
                        Math.max(r.getMinOrderAmount(), z.getMinOrderAmount()));
            }
        }
        return new DeliveryQuote(r.getDefaultDeliveryFee(), fallbackEta, false, r.getMinOrderAmount());
    }
}
