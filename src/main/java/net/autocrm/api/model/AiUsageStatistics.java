package net.autocrm.api.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 사용 통계 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageStatistics {
    private String brand;
    private String dealerCode;
    private String period;
    private Integer totalRequests;
    private Integer successfulRequests;
    private Integer failedRequests;
    private Double successRate;
    private Double avgResponseTime;
    private Integer uniqueUsers;
    private Integer totalSessions;
    private Map<String, Integer> hourlyDistribution;
    private Map<String, Integer> categoryDistribution;
    private List<String> topKeywords;
    private LocalDateTime createdAt;
}

