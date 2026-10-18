package com.drippay.controller;

import com.drippay.merchant.WebhookService;
import com.drippay.security.SecurityUtils;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/merchant")
public class MerchantController {

    private final WebhookService webhookService;

    public MerchantController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/webhooks")
    public WebhookService.WebhookEndpointResponse register(@RequestBody Map<String, @NotBlank String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            throw new com.drippay.exception.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "url is required");
        }
        return webhookService.register(SecurityUtils.currentUser().getId(), url);
    }

    @GetMapping("/webhooks")
    public List<WebhookService.WebhookEndpointResponse> list() {
        return webhookService.list(SecurityUtils.currentUser().getId());
    }
}
