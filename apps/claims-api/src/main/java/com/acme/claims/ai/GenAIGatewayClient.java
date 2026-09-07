package com.acme.claims.ai;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * GenAI gateway client (ADR-004, ADR-005, ADR-011). The gateway is
 * vendor-neutral (LiteLLM) and advisory only: its assessments feed human
 * decisions and are never executed automatically.
 *
 * <p>The only route used is the OpenAI-compatible chat completion; domain
 * prompts (e.g. triage assessment) are composed by callers on top of it.</p>
 */
@FeignClient(name = "genai-gateway", url = "${genai.gateway.url:http://localhost:9000}")
public interface GenAIGatewayClient {

    @PostMapping("/v1/chat/completions")
    GenAIResponse chatCompletion(@RequestHeader("Authorization") String apiKey, @RequestBody GenAIRequest request);

    record GenAIRequest(String model, List<Message> messages, Double temperature, Integer maxTokens) {

        public static GenAIRequest create(String model, List<Message> messages) {
            return new GenAIRequest(model, messages, 0.7, 1024);
        }
    }

    record Message(String role, String content) {

        public static Message system(String content) {
            return new Message("system", content);
        }

        public static Message user(String content) {
            return new Message("user", content);
        }

        public static Message assistant(String content) {
            return new Message("assistant", content);
        }
    }

    record GenAIResponse(String id, List<Choice> choices, Usage usage, long created) {

        public record Choice(int index, Message message, String finishReason) {
        }

        public record Usage(int promptTokens, int completionTokens, int totalTokens) {
        }

        public String getContent() {
            return choices == null || choices.isEmpty() ? null : choices.getFirst().message().content();
        }
    }
}
