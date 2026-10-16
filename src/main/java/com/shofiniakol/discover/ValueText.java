package com.shofiniakol.discover;

import com.shofiniakol.common.GeoUtils;
import com.shofiniakol.common.Lang;
import com.shofiniakol.common.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Human-readable (ar/en/fr) explanations of a value score: "why is this a good deal?". */
public final class ValueText {

    private ValueText() {
    }

    public static String label(int score) {
        if (score >= 85) {
            return Lang.pick("قيمة ممتازة", "Excellent value", "Excellente valeur");
        }
        if (score >= 70) {
            return Lang.pick("قيمة جيدة", "Good value", "Bonne valeur");
        }
        if (score >= 50) {
            return Lang.pick("قيمة مقبولة", "Fair value", "Valeur correcte");
        }
        return Lang.pick("قيمة عادية", "Average value", "Valeur moyenne");
    }

    public static String componentLabel(String key) {
        return switch (key) {
            case "discount" -> Lang.pick("الخصم", "Discount", "Remise");
            case "rating" -> Lang.pick("التقييم", "Rating", "Note");
            case "portion" -> Lang.pick("الكمية", "Portion", "Portion");
            case "delivery" -> Lang.pick("التوصيل", "Delivery", "Livraison");
            case "distance" -> Lang.pick("المسافة", "Distance", "Distance");
            case "budget_fit" -> Lang.pick("ملاءمة الميزانية", "Budget fit", "Adéquation au budget");
            default -> key;
        };
    }

    public static List<String> reasons(ScoredOffer s, Long budget) {
        List<String> out = new ArrayList<>();
        long price = s.offer().getPrice();
        long original = s.offer().getOriginalPrice();
        int pct = Money.discountPercent(price, original);
        long saving = Money.saving(price, original);

        if (budget != null && price <= budget) {
            out.add(Lang.pick("ضمن ميزانيتك", "Within your budget", "Dans votre budget"));
        }
        if (pct >= 15) {
            out.add(Lang.pick("خصم " + pct + "%", pct + "% off", pct + " % de réduction"));
        }
        if (saving > 0) {
            String n = fmt(saving);
            out.add(Lang.pick("توفّر " + n + " ل.س", "Save " + n + " SYP", "Économisez " + n + " SYP"));
        }
        if (s.effectiveRating() >= 4.3) {
            String r = String.format(Locale.ENGLISH, "%.1f", s.effectiveRating());
            out.add(Lang.pick("تقييم " + r, "Rated " + r, "Note " + r));
        }
        if (s.value().scoreOf("portion") >= 0.6) {
            out.add(Lang.pick("كمية مشبعة بسعرها", "Generous portion for the price", "Portion généreuse pour le prix"));
        }
        if (s.quote().fee() == 0) {
            out.add(Lang.pick("توصيل مجاني", "Free delivery", "Livraison gratuite"));
        }
        if (s.distanceKm() != null && s.distanceKm() <= 2.0) {
            String d = String.format(Locale.ENGLISH, "%.1f", GeoUtils.round1(s.distanceKm()));
            out.add(Lang.pick("قريب منك (" + d + " كم)", "Close to you (" + d + " km)", "Près de vous (" + d + " km)"));
        }
        return out;
    }

    private static String fmt(long n) {
        return String.format(Locale.ENGLISH, "%,d", n);
    }
}
