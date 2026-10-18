package com.drippay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "drippay")
public class DrippayProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Fraud fraud = new Fraud();
    private final Ai ai = new Ai();
    private final Razorpay razorpay = new Razorpay();

    public Jwt getJwt() {
        return jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public Fraud getFraud() {
        return fraud;
    }

    public Ai getAi() {
        return ai;
    }

    public Razorpay getRazorpay() {
        return razorpay;
    }

    public static class Jwt {
        private String secret;
        private long expirationMs;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }

    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Fraud {
        private int maxFailedTransfersPerHour = 5;
        private long highAmountThreshold = 10000;
        private int rapidTxWindowMinutes = 10;
        private int rapidTxCount = 8;

        public int getMaxFailedTransfersPerHour() {
            return maxFailedTransfersPerHour;
        }

        public void setMaxFailedTransfersPerHour(int maxFailedTransfersPerHour) {
            this.maxFailedTransfersPerHour = maxFailedTransfersPerHour;
        }

        public long getHighAmountThreshold() {
            return highAmountThreshold;
        }

        public void setHighAmountThreshold(long highAmountThreshold) {
            this.highAmountThreshold = highAmountThreshold;
        }

        public int getRapidTxWindowMinutes() {
            return rapidTxWindowMinutes;
        }

        public void setRapidTxWindowMinutes(int rapidTxWindowMinutes) {
            this.rapidTxWindowMinutes = rapidTxWindowMinutes;
        }

        public int getRapidTxCount() {
            return rapidTxCount;
        }

        public void setRapidTxCount(int rapidTxCount) {
            this.rapidTxCount = rapidTxCount;
        }
    }

    public static class Ai {
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4o-mini";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public boolean isEnabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public static class Razorpay {
        private String keyId = "";
        private String keySecret = "";
        private boolean simulated = true;

        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
        public String getKeySecret() { return keySecret; }
        public void setKeySecret(String keySecret) { this.keySecret = keySecret; }
        public boolean isSimulated() { return simulated; }
        public void setSimulated(boolean simulated) { this.simulated = simulated; }
        public boolean isLiveConfigured() {
            return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
        }
    }
}
