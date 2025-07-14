package net.autocrm.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 메뉴 정보 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuInfo {
    private String menuId;
    private String menuName;
    private String menuUrl;
    private String description;
    private String requiredRole;
    private String parentMenuId;
    private boolean isActive;
}

