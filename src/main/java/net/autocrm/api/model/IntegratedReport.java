package net.autocrm.api.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 통합 리포트 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedReport {
    private Long id;
    private String reportType;
    private String reportTitle;
    private String brand;
    private String dealerCode;
    private String period;
    private String reportData;
    private String summary;
    private String recommendations;
    private String generatedBy;
    private LocalDateTime generatedAt;
    private String isActive;
}

