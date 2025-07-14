package net.autocrm.api.model;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 대화 기록 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationHistory {
    private Long id;
    private Integer clientSeq;
    private String sessionId;
    private String userMessage;
    private String botResponse;
    private String category;
    private LocalDateTime createdAt;
    private Map<String, Object> metadata;
}

