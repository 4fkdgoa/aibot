package net.autocrm.api.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 통합 대화 기록 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedConversationHistory {
    private Long id;
    private String brand;
    private String dealerCode;
    private String salesUserSeq;
    private String userName;
    private String dealerSeq;
    private String showroomSeq;
    private String teamSeq;
    private String authGroup;
    private String sessionId;
    private String userMessage;
    private String botResponse;
    private String category;
    private String actionType;
    private String actionData;
    private Integer processingTimeMs;
    private String successYn;
    private Integer rating;
    private String feedback;
    private String requestData;
    private String responseData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

