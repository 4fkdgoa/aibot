package net.autocrm.api.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 설정 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfig {
    private String configKey;
    private String configValue;
    private String description;
    private String category;
    private boolean isActive;
    private LocalDateTime updatedAt;
}

