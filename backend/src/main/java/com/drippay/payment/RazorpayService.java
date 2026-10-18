package com.drippay.payment;

import com.drippay.config.DrippayProperties;
import com.drippay.domain.entity.RazorpayOrder;
import com.drippay.dto.payment.RazorpayOrderResponse;
import com.drippay.dto.payment.RazorpayVerifyRequest;
import com.drippay.dto.transaction.AmountRequest;
import com.drippay.dto.transaction.TransactionResponse;
import com.drippay.exception.ApiException;
import com.drippay.repository.RazorpayOrderRepository;
import com.drippay.service.TransferService;
import com.drippay.util.MoneyUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class RazorpayService {

    private final DrippayProperties properties;
    private final RazorpayOrderRepository orderRepository;
    private final TransferService transferService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public RazorpayService(DrippayProperties properties,
                           RazorpayOrderRepository orderRepository,
                           TransferService transferService) {
        this.properties = properties;
        this.orderRepository = orderRepository;
        this.transferService = transferService;
    }

    @Transactional
    public RazorpayOrderResponse createOrder(Long userId, BigDecimal amountInr) {
        BigDecimal amount = MoneyUtils.normalize(amountInr);
        MoneyUtils.requirePositive(amount);
        long paise = amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();

        boolean simulated = !properties.getRazorpay().isLiveConfigured() || properties.getRazorpay().isSimulated();
        String orderId;
        String keyId = properties.getRazorpay().getKeyId();

        if (simulated) {
            orderId = "order_sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            if (keyId == null || keyId.isBlank()) {
                keyId = "rzp_test_simulated";
            }
        } else {
            try {
                orderId = createLiveOrder(paise);
            } catch (Exception e) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Razorpay order failed: " + e.getMessage());
            }
        }

        RazorpayOrder row = new RazorpayOrder();
        row.setUserId(userId);
        row.setRazorpayOrderId(orderId);
        row.setAmountPaise(paise);
        row.setSimulated(simulated);
        row.setStatus("CREATED");
        orderRepository.save(row);

        return new RazorpayOrderResponse(orderId, keyId, paise, "INR", simulated);
    }

    @Transactional
    public TransactionResponse verifyAndCredit(Long userId, RazorpayVerifyRequest body, String ip) {
        RazorpayOrder order = orderRepository.findByRazorpayOrderIdAndUserId(body.orderId(), userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        if ("PAID".equals(order.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Order already paid");
        }

        String paymentId = body.paymentId();
        String signature = body.signature();
        if (order.isSimulated()) {
            if (paymentId == null || paymentId.isBlank()) {
                paymentId = "pay_sim_" + UUID.randomUUID();
                signature = "simulated";
            }
        } else {
            if (!verifyLiveSignature(body.orderId(), paymentId, signature)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid payment signature");
            }
        }

        order.setStatus("PAID");
        order.setPaidAt(Instant.now());
        orderRepository.save(order);

        BigDecimal inr = BigDecimal.valueOf(order.getAmountPaise()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return transferService.demoDeposit(userId, new AmountRequest(inr, "Razorpay top-up"), ip);
    }

    private String createLiveOrder(long amountPaise) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "amount", amountPaise,
                "currency", "INR",
                "receipt", "drippay_" + UUID.randomUUID().toString().substring(0, 8)
        ));
        String auth = Base64.getEncoder().encodeToString(
                (properties.getRazorpay().getKeyId() + ":" + properties.getRazorpay().getKeySecret())
                        .getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.razorpay.com/v1/orders"))
                .header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Razorpay HTTP " + response.statusCode());
        }
        JsonNode json = objectMapper.readTree(response.body());
        return json.get("id").asText();
    }

    private boolean verifyLiveSignature(String orderId, String paymentId, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getRazorpay().getKeySecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return expected.equalsIgnoreCase(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
