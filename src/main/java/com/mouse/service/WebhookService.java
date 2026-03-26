package com.mouse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouse.model.PullRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    @Value("${github.webhook.secret}")
    private String webhookSecret;

    private final ObjectMapper objectMapper;

    // 1. Security: HMAC SHA-256 Validation
    public boolean isValidSignature(String payload, String signature) {

        if (signature == null || !signature.startsWith("sha256=")) {
            log.warn("Invalid GitHub signature format");
            return false;
        }

        try {
            String actualSignature = signature.substring(7);

            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);

            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = HexFormat.of().formatHex(bytes);

            boolean isValid = computedSignature.equalsIgnoreCase(actualSignature);

            if (!isValid) {
                log.warn("GitHub webhook signature validation failed");
            } else {
                log.info("GitHub webhook signature validated successfully");
            }

            return isValid;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error validating GitHub webhook signature", e);
            return false;
        }
    }

    // 2. Transformation: Mapping JSON to PullRequestEvent DTO
    public PullRequestEvent parseEvent(String payload) {
        try {
            PullRequestEvent event = objectMapper.readValue(payload, PullRequestEvent.class);

            log.info("Successfully parsed GitHub webhook event: action={}, repo={}",
                    event.action(),
                    event.repository() != null ? event.repository().name() : "unknown");

            return event;

        } catch (Exception e) {
            log.error("Failed to parse GitHub Webhook payload", e);
            throw new RuntimeException("Failed to parse GitHub Webhook payload", e);
        }
    }
}