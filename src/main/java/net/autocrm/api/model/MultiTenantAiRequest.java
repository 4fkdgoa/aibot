package net.autocrm.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 멀티 테넌트 AI 요청 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiTenantAiRequest {
    private String message;
    private String sessionId;
    private String brand;           // B, M, U, S 등
    private String dealerCode;      // 딜러 코드
    private String salesUserSeq;    // 사용자 SEQ
    private Map<String, Object> context;
}

