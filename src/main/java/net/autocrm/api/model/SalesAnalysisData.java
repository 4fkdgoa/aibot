package net.autocrm.api.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 판매 분석 데이터 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesAnalysisData {
    private Integer totalContracts;
    private Integer totalCancellations;
    private Integer totalDeliveries;
    private Integer totalPayments;
    private Integer totalUnpaid;
    private Double contractAmount;
    private Double paidAmount;
    private Double unpaidAmount;
    private Map<String, Object> monthlyData;
    private Map<String, Object> categoryData;
}

