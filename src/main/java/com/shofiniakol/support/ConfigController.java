package com.shofiniakol.support;

import com.shofiniakol.common.Lang;
import com.shofiniakol.config.AppProperties;
import com.shofiniakol.order.DeliveryType;
import com.shofiniakol.order.OrderStatus;
import com.shofiniakol.order.PaymentMethod;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/** Bootstrap data for the mobile app: support contacts, payment options, labels, presets. */
@Tag(name = "Config")
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class ConfigController {

    private final AppProperties props;

    public record Support(String whatsapp, String whatsappUrl, String phone) {
    }

    public record Legal(String termsUrl, String privacyUrl) {
    }

    public record PaymentMethodDto(PaymentMethod code, String label, String account, String instructions,
                                   boolean requiresReference) {
    }

    public record DeliveryTypeDto(DeliveryType code, String label) {
    }

    public record StatusDto(OrderStatus code, String label, String tab) {
    }

    public record AppConfig(String appVersion, String currency, String currencySymbol, String timezone,
                            List<String> languages, List<Long> budgetPresets, Support support, Legal legal,
                            List<PaymentMethodDto> paymentMethods, List<DeliveryTypeDto> deliveryTypes,
                            List<StatusDto> orderStatuses) {
    }

    @GetMapping
    public AppConfig config() {
        var s = props.getSupport();
        String sham = props.getPayments().getShamCashAccount();
        return new AppConfig(s.getAppVersion(), "SYP", "ل.س", props.getTimezone(), List.of("ar", "en", "fr"),
                props.getDiscover().getBudgetPresets(),
                new Support(s.getWhatsapp(), "https://wa.me/" + s.getWhatsapp().replace("+", ""), s.getPhone()),
                new Legal(s.getTermsUrl(), s.getPrivacyUrl()),
                List.of(
                        new PaymentMethodDto(PaymentMethod.CASH_ON_DELIVERY,
                                Lang.pick("دفع عند الاستلام", "Cash on delivery", "Paiement à la livraison"),
                                null, null, false),
                        new PaymentMethodDto(PaymentMethod.SHAM_CASH,
                                Lang.pick("شام كاش", "Sham Cash", "Sham Cash"),
                                sham == null || sham.isBlank() ? null : sham,
                                Lang.pick("حوّل المبلغ ثم اكتب رقم العملية",
                                        "Transfer the amount, then enter the transaction number",
                                        "Transférez le montant puis saisissez le numéro de transaction"),
                                true)),
                List.of(
                        new DeliveryTypeDto(DeliveryType.DELIVERY,
                                Lang.pick("توصيل للمنزل", "Home delivery", "Livraison à domicile")),
                        new DeliveryTypeDto(DeliveryType.PICKUP,
                                Lang.pick("استلام من المطعم", "Pickup from restaurant", "Retrait au restaurant"))),
                Arrays.stream(OrderStatus.values()).map(o -> new StatusDto(o, o.label(), o.tab())).toList());
    }
}
