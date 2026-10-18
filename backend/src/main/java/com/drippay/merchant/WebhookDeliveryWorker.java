package com.drippay.merchant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Component
public class WebhookDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryWorker.class);
    private static final int MAX_ATTEMPTS = 5;

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookEndpointRepository endpointRepository;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public WebhookDeliveryWorker(WebhookDeliveryRepository deliveryRepository,
                                 WebhookEndpointRepository endpointRepository) {
        this.deliveryRepository = deliveryRepository;
        this.endpointRepository = endpointRepository;
    }

    @Scheduled(fixedDelayString = "${drippay.webhook.poll-ms:7000}")
    @Transactional
    public void deliverPending() {
        List<WebhookDelivery> pending = deliveryRepository.findTop30ByStatusOrderByCreatedAtAsc("PENDING");
        for (WebhookDelivery delivery : pending) {
            WebhookEndpoint endpoint = endpointRepository.findById(delivery.getEndpointId()).orElse(null);
            if (endpoint == null || !endpoint.isActive()) {
                delivery.setStatus("FAILED");
                delivery.setLastError("Endpoint missing or inactive");
                continue;
            }
            String signature = WebhookService.hmacSha256(endpoint.getSecret(), delivery.getPayload());
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint.getUrl()))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .header("X-DripPay-Event", delivery.getEventType())
                        .header("X-DripPay-Signature", signature)
                        .POST(HttpRequest.BodyPublishers.ofString(delivery.getPayload()))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    delivery.setStatus("DELIVERED");
                    log.info("Webhook delivered id={} status={}", delivery.getId(), response.statusCode());
                } else {
                    failAttempt(delivery, "HTTP " + response.statusCode());
                }
            } catch (Exception ex) {
                failAttempt(delivery, ex.getMessage());
            }
        }
    }

    private void failAttempt(WebhookDelivery delivery, String error) {
        delivery.setAttempts(delivery.getAttempts() + 1);
        delivery.setLastError(error != null && error.length() > 500 ? error.substring(0, 500) : error);
        if (delivery.getAttempts() >= MAX_ATTEMPTS) {
            delivery.setStatus("FAILED");
        }
    }
}
