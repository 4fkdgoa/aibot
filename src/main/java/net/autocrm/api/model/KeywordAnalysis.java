package net.autocrm.api.model;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 키워드 분석 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordAnalysis {
    private Long id;
    private String keyword;
    private String category;
    private Integer frequency;
    private String userRole;
    private LocalDateTime firstUsed;
    private LocalDateTime lastUsed;
    private Map<String, Object> analysisData;
}

