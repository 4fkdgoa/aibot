package net.autocrm.api.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 통합 키워드 분석 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedKeywordAnalysis {
    private Long id;
    private String keyword;
    private String brand;
    private String dealerCode;
    private String userRole;
    private String dealerSeq;
    private String showroomSeq;
    private String teamSeq;
    private String category;
    private Integer frequency;
    private Integer successCount;
    private Integer failureCount;
    private LocalDateTime firstUsed;
    private LocalDateTime lastUsed;
    private String analysisData;
    private String isActive;
}

