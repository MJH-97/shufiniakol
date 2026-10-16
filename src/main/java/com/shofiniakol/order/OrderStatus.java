package com.shofiniakol.order;

import com.shofiniakol.common.Lang;

import java.util.Set;

public enum OrderStatus {
    PENDING("بانتظار التأكيد", "Pending confirmation", "En attente de confirmation"),
    CONFIRMED("تم التأكيد", "Confirmed", "Confirmée"),
    PREPARING("قيد التحضير", "Preparing", "En préparation"),
    OUT_FOR_DELIVERY("خرج للتوصيل", "Out for delivery", "En livraison"),
    READY_FOR_PICKUP("جاهز للاستلام", "Ready for pickup", "Prête à retirer"),
    DELIVERED("تم التوصيل", "Delivered", "Livrée"),
    CANCELLED("ملغي", "Cancelled", "Annulée");

    private final String ar;
    private final String en;
    private final String fr;

    OrderStatus(String ar, String en, String fr) {
        this.ar = ar;
        this.en = en;
        this.fr = fr;
    }

    public String label() {
        return Lang.pick(ar, en, fr);
    }

    /** The three tabs of the "My orders" screen: قيد التنفيذ / مكتملة / ملغاة. */
    public String tab() {
        return switch (this) {
            case DELIVERED -> "completed";
            case CANCELLED -> "cancelled";
            default -> "active";
        };
    }

    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED;
    }

    public static Set<OrderStatus> forTab(String tab) {
        return switch (tab == null ? "all" : tab.toLowerCase()) {
            case "active" -> Set.of(PENDING, CONFIRMED, PREPARING, OUT_FOR_DELIVERY, READY_FOR_PICKUP);
            case "completed" -> Set.of(DELIVERED);
            case "cancelled" -> Set.of(CANCELLED);
            default -> Set.of(values());
        };
    }

    public boolean canMoveTo(OrderStatus next, DeliveryType type) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == PREPARING || next == CANCELLED;
            case PREPARING -> next == CANCELLED
                    || (type == DeliveryType.DELIVERY && next == OUT_FOR_DELIVERY)
                    || (type == DeliveryType.PICKUP && next == READY_FOR_PICKUP);
            case OUT_FOR_DELIVERY, READY_FOR_PICKUP -> next == DELIVERED || next == CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
