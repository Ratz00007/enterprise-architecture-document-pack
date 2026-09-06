package com.acme.claims.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * PII Masking Service - Masks sensitive data before syncing to analytics database
 * 
 * Implements two-database sync with PII masking as per ADR-013
 */
@Service
@Slf4j
public class PIIMaskingService {

    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern CC_PATTERN = Pattern.compile("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b");

    /**
     * Mask all PII fields in the given text
     */
    public String maskPII(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String masked = input;

        // Mask Social Security Numbers
        masked = SSN_PATTERN.matcher(masked).replaceAll("***-**-****");

        // Mask Phone Numbers
        masked = PHONE_PATTERN.matcher(masked).replaceAll("***-***-****");

        // Mask Email Addresses
        masked = EMAIL_PATTERN.matcher(masked).replaceAll("***@***.***");

        // Mask Credit Card Numbers
        masked = CC_PATTERN.matcher(masked).replaceAll("****-****-****-****");

        log.debug("PII masking applied to text of length: {}", input.length());
        return masked;
    }

    /**
     * Mask a specific field value based on field type
     */
    public String maskField(String value, String fieldType) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        return switch (fieldType.toLowerCase()) {
            case "ssn", "social_security" -> "***-**-****";
            case "phone", "telephone", "mobile" -> "***-***-****";
            case "email", "e-mail" -> "***@***.***";
            case "credit_card", "cc", "card_number" -> "****-****-****-****";
            case "name" -> maskName(value);
            case "address" -> maskAddress(value);
            default -> maskPII(value);
        };
    }

    /**
     * Mask name - keep first letter only
     */
    public String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        String[] parts = name.split(" ");
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) masked.append(" ");
            if (parts[i].length() > 0) {
                masked.append(parts[i].charAt(0));
                masked.append("****");
            }
        }
        return masked.toString();
    }

    /**
     * Mask address - keep only city and state
     */
    public String maskAddress(String address) {
        if (address == null || address.isEmpty()) {
            return address;
        }
        // Simple implementation - replace street number and name
        return address.replaceAll("\\d+\\s+[A-Za-z\\s]+,", "[REDACTED],");
    }

    /**
     * Check if text contains any PII
     */
    public boolean containsPII(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return SSN_PATTERN.matcher(text).find() ||
               PHONE_PATTERN.matcher(text).find() ||
               EMAIL_PATTERN.matcher(text).find() ||
               CC_PATTERN.matcher(text).find();
    }
}
