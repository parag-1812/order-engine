package com.cloudkitchen.order_engine.service;

import com.cloudkitchen.order_engine.dto.PaymentCreateResponse;
import com.cloudkitchen.order_engine.dto.VerifyPaymentRequest;
import com.cloudkitchen.order_engine.order.OrderEntity;
import com.cloudkitchen.order_engine.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Razorpay payments via the REST API (Basic auth) + HMAC-SHA256 signature
 * verification. No vendor SDK. Keys come from RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET.
 */
@Service
public class PaymentService {

    private static final String RAZORPAY_ORDERS_URL = "https://api.razorpay.com/v1/orders";

    private final OrderRepository orderRepository;
    private final ObjectMapper mapper;
    private final String keyId;
    private final String keySecret;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public PaymentService(
            OrderRepository orderRepository,
            ObjectMapper mapper,
            @Value("${razorpay.key-id:}") String keyId,
            @Value("${razorpay.key-secret:}") String keySecret) {
        this.orderRepository = orderRepository;
        this.mapper = mapper;
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    @Transactional
    public PaymentCreateResponse createPayment(Long orderId) {
        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new IllegalStateException(
                    "Razorpay keys are not set (RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET).");
        }

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        long amountPaise = Math.round(order.getTotalPrice() * 100);

        try {
            String body = mapper.writeValueAsString(Map.of(
                    "amount", amountPaise,
                    "currency", "INR",
                    "receipt", "order_" + orderId));

            String auth = Base64.getEncoder()
                    .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RAZORPAY_ORDERS_URL))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Razorpay error " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            String razorpayOrderId = root.path("id").asString();

            order.setRazorpayOrderId(razorpayOrderId);
            orderRepository.save(order);

            return new PaymentCreateResponse(orderId, keyId, razorpayOrderId, amountPaise, "INR");

        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to reach Razorpay: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling Razorpay", e);
        }
    }

    @Transactional
    public void verifyAndMarkPaid(Long orderId, VerifyPaymentRequest req) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        String expected = hmacSha256Hex(
                req.getRazorpayOrderId() + "|" + req.getRazorpayPaymentId(), keySecret);

        if (!expected.equals(req.getRazorpaySignature())) {
            throw new IllegalArgumentException("Payment signature verification failed");
        }

        order.setPaymentStatus("PAID");
        orderRepository.save(order);
    }

    private String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }
}
