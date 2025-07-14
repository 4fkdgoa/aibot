package net.autocrm.api.model;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 챗봇 응답 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    private String message;
    private String actionType; // NAVIGATE, CHART, SEARCH, RESPONSE, ERROR
    private Map<String, Object> actionData;
    private LocalDateTime timestamp;
    private String sessionId;
    
    @Builder.Default
    private boolean success = true;
}

