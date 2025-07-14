package net.autocrm.api.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 브랜드별 판매 데이터 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandSalesData {
    private String brand;
    private String dealerCode;
    private String period;
    private Integer totalContracts;
    private Integer completedContracts;
    private Integer cancelledContracts;
    private Integer deliveredContracts;
    private Double totalAmount;
    private Double paidAmount;
    private Double unpaidAmount;
    private Map<String, Object> monthlyData;
    private Map<String, Object> modelData;
    private Map<String, Object> agentData;
}

