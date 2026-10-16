package com.drippay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "drippay")
public class DrippayProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Fraud fraud = new Fraud();

    public Jwt getJwt() {
        return jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public Fraud getFraud() {
        return fraud;
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
}
