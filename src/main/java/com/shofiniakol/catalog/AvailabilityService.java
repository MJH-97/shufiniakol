package com.shofiniakol.catalog;

import com.shofiniakol.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/** Time-based rules (all evaluated in the business timezone, Asia/Damascus by default). */
@Service
public class AvailabilityService {

    private final ZoneId zone;

    public AvailabilityService(AppProperties props) {
        this.zone = ZoneId.of(props.getTimezone());
    }

    public boolean isRestaurantOpen(Restaurant r, Instant now) {
        return r.isOpenAt(now.atZone(zone).toLocalTime());
    }

    /** Active, inside its date window, in stock, and inside its weekly time window (if any). */
    public boolean isOfferAvailable(Offer o, Instant now) {
        if (!o.isActive()) {
            return false;
        }
        if (o.getStartsAt() != null && o.getStartsAt().isAfter(now)) {
            return false;
        }
        if (o.getEndsAt() != null && !o.getEndsAt().isAfter(now)) {
            return false;
        }
        if (o.getStockRemaining() != null && o.getStockRemaining() <= 0) {
            return false;
        }
        List<OfferAvailability> windows = o.getAvailability();
        if (windows.isEmpty()) {
            return true;
        }
        ZonedDateTime z = now.atZone(zone);
        int dow = z.getDayOfWeek().getValue();
        LocalTime t = z.toLocalTime();
        for (OfferAvailability w : windows) {
            LocalTime s = w.getStartTime();
            LocalTime e = w.getEndTime();
            if (s.equals(e)) {
                if (w.getDayOfWeek() == dow) {
                    return true;
                }
            } else if (s.isBefore(e)) {
                if (w.getDayOfWeek() == dow && !t.isBefore(s) && t.isBefore(e)) {
                    return true;
                }
            } else { // wraps past midnight: covers [s, 24:00) today and [00:00, e) tomorrow
                int next = w.getDayOfWeek() % 7 + 1;
                if ((w.getDayOfWeek() == dow && !t.isBefore(s)) || (next == dow && t.isBefore(e))) {
                    return true;
                }
            }
        }
        return false;
    }
}
