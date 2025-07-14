package net.autocrm.api.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 딜러 성과 비교 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealerPerformanceComparison {
    private String dealerCode;
    private String dealerName;
    private String brand;
    private Integer totalAiRequests;
    private Integer uniqueUsers;
    private Integer totalConversations;
    private Double avgResponseTime;
    private Double satisfactionScore;
    private List<String> topKeywords;
    private Map<String, Integer> categoryDistribution;
    private LocalDateTime lastActivity;
}

