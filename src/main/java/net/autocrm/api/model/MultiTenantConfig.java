package net.autocrm.api.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 멀티 테넌트 설정 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiTenantConfig {
    private String brand;
    private String dealerCode;
    private String dbName;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String configKey;
    private String configValue;
    private String description;
    private String isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}