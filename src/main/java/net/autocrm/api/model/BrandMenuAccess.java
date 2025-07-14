package net.autocrm.api.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 브랜드별 메뉴 접근 권한 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandMenuAccess {
    private String brand;
    private String menuId;
    private String menuName;
    private String menuUrl;
    private String requiredAuthGroup;
    private String accessLevel;
    private String isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

