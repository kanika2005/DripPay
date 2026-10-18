package com.drippay.merchant;

import com.drippay.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WebhookService {

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;

    public WebhookService(WebhookEndpointRepository endpointRepository,
                          WebhookDeliveryRepository deliveryRepository,
                          ObjectMapper objectMapper) {
        this.endpointRepository = endpointRepository;
        this.deliveryRepository = deliveryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WebhookEndpointResponse register(Long userId, String url) {
        WebhookEndpoint endpoint = new WebhookEndpoint();
        endpoint.setUserId(userId);
        endpoint.setUrl(url);
        endpoint.setSecret(UUID.randomUUID().toString().replace("-", ""));
        endpoint.setActive(true);
        endpoint = endpointRepository.save(endpoint);
        return new WebhookEndpointResponse(endpoint.getId(), endpoint.getUrl(), endpoint.getSecret(), endpoint.isActive());
    }

    @Transactional(readOnly = true)
    public List<WebhookEndpointResponse> list(Long userId) {
        return endpointRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(e -> new WebhookEndpointResponse(e.getId(), e.getUrl(), null, e.isActive()))
                .toList();
    }

    @Transactional
    public void enqueueForUser(Long merchantUserId, String eventType, Map<String, Object> body) {
        List<WebhookEndpoint> endpoints = endpointRepository.findByUserIdAndActiveTrue(merchantUserId);
        if (endpoints.isEmpty()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(body);
            for (WebhookEndpoint endpoint : endpoints) {
                WebhookDelivery delivery = new WebhookDelivery();
                delivery.setEndpointId(endpoint.getId());
                delivery.setEventType(eventType);
                delivery.setPayload(payload);
                delivery.setStatus("PENDING");
                delivery.setAttempts(0);
                deliveryRepository.save(delivery);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Webhook payload error", e);
        }
    }

    public static String hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "HMAC failed");
        }
    }

    public record WebhookEndpointResponse(Long id, String url, String secret, boolean active) {
    }
}
