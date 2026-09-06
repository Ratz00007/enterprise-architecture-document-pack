package com.acme.claims.ai;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * GenAI Gateway Client - Central gateway for all GenAI provider interactions
 * 
 * Supports provider abstraction with fallback mechanisms as per ADR-015
 */
@Component
@FeignClient(name = "genai-gateway", url = "${genai.gateway.url}")
public interface GenAIGatewayClient {

    @PostMapping("/v1/chat/completions")
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000)
    )
    GenAIResponse chatCompletion(
        @RequestHeader("Authorization") String apiKey,
        @RequestBody GenAIRequest request
    );

    @PostMapping("/v1/triage/assess")
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000)
    )
    TriageAssessment assessClaim(
        @RequestHeader("Authorization") String apiKey,
        @RequestBody ClaimAssessmentRequest request
    );

    @Data
    class GenAIRequest {
        private String model;
        private List<Message> messages;
        private Double temperature;
        private Integer maxTokens;

        public static GenAIRequest create(String model, List<Message> messages) {
            GenAIRequest request = new GenAIRequest();
            request.setModel(model);
            request.setMessages(messages);
            request.setTemperature(0.7);
            request.setMaxTokens(1024);
            return request;
        }
    }

    @Data
    class Message {
        private String role;
        private String content;

        public static Message system(String content) {
            Message msg = new Message();
            msg.setRole("system");
            msg.setContent(content);
            return msg;
        }

        public static Message user(String content) {
            Message msg = new Message();
            msg.setRole("user");
            msg.setContent(content);
            return msg;
        }

        public static Message assistant(String content) {
            Message msg = new Message();
            msg.setRole("assistant");
            msg.setContent(content);
            return msg;
        }
    }

    @Data
    class GenAIResponse {
        private String id;
        private Choice[] choices;
        private Usage usage;
        private long created;

        @Data
        class Choice {
            private int index;
            private Message message;
            private String finishReason;
        }

        @Data
        class Usage {
            private int promptTokens;
            private int completionTokens;
            private int totalTokens;
        }

        public String getContent() {
            if (choices != null && choices.length > 0) {
                return choices[0].getMessage().getContent();
            }
            return null;
        }
    }

    @Data
    class ClaimAssessmentRequest {
        private String claimId;
        private String claimType;
        private String description;
        private Double estimatedAmount;
        private String policyDetails;

        public static ClaimAssessmentRequest create(
            String claimId,
            String claimType,
            String description,
            Double estimatedAmount,
            String policyDetails
        ) {
            ClaimAssessmentRequest request = new ClaimAssessmentRequest();
            request.setClaimId(claimId);
            request.setClaimType(claimType);
            request.setDescription(description);
            request.setEstimatedAmount(estimatedAmount);
            request.setPolicyDetails(policyDetails);
            return request;
        }
    }

    @Data
    class TriageAssessment {
        private String claimId;
        private Integer priority;
        private Double score;
        private String recommendation;
        private String[] requiredDocuments;
        private boolean requiresHumanReview;
        private String reasoning;
    }
}
