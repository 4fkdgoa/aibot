package net.autocrm.api.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 멀티 테넌트 컨텍스트 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiTenantContext {
    private String brand;
    private String dealerCode;
    private String salesUserSeq;
    private String userName;
    private String dealerSeq;
    private String showroomSeq;
    private String teamSeq;
    private String authGroup;
    private String authSeq;
    private String gradeName;
    private String dutyName;
    private String dbName;
    private LocalDateTime createdAt;
}

