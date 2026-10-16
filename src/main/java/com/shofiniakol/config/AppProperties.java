package com.shofiniakol.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/** Typed view of the {@code app.*} section of application.yml. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String timezone = "Asia/Damascus";
    private String publicBaseUrl = "http://localhost:8080";
    private Security security = new Security();
    private Otp otp = new Otp();
    private Bootstrap bootstrap = new Bootstrap();
    private Discover discover = new Discover();
    private Delivery delivery = new Delivery();
    private Storage storage = new Storage();
    private Support support = new Support();
    private Payments payments = new Payments();
    private Jobs jobs = new Jobs();

    @Getter
    @Setter
    public static class Security {
        private String jwtSecret;
        private int accessTokenTtlMinutes = 30;
        private int refreshTokenTtlDays = 30;
        private String corsAllowedOrigins = "*";
    }

    @Getter
    @Setter
    public static class Otp {
        private int length = 6;
        private int ttlSeconds = 300;
        private int resendCooldownSeconds = 60;
        private int maxAttempts = 5;
        private int maxPerHour = 6;
        private String pepper = "dev-pepper";
        private String devFixedCode = "";
    }

    @Getter
    @Setter
    public static class Bootstrap {
        private String adminPhone = "";
    }

    @Getter
    @Setter
    public static class Discover {
        private List<Long> budgetPresets = new ArrayList<>(List.of(20000L, 30000L, 50000L, 75000L, 100000L));
        private double maxDistanceKm = 10;
        private int limitedTimeHours = 48;
        private Weights weights = new Weights();
    }

    @Getter
    @Setter
    public static class Weights {
        private double discount = 0.25;
        private double rating = 0.25;
        private double portion = 0.20;
        private double delivery = 0.10;
        private double distance = 0.10;
        private double budgetFit = 0.10;
    }

    @Getter
    @Setter
    public static class Delivery {
        private double defaultMaxDistanceKm = 15;
        private int defaultEtaExtraMinutes = 15;
    }

    @Getter
    @Setter
    public static class Storage {
        private String dir = "./uploads";
    }

    @Getter
    @Setter
    public static class Support {
        private String whatsapp = "";
        private String phone = "";
        private String termsUrl = "";
        private String privacyUrl = "";
        private String appVersion = "1.0.0";
    }

    @Getter
    @Setter
    public static class Payments {
        private String shamCashAccount = "";
    }

    @Getter
    @Setter
    public static class Jobs {
        private long expiryCheckMs = 300000;
        private int expiryWindowHours = 2;
    }
}
