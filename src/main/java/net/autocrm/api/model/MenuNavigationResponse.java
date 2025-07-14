package net.autocrm.api.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuNavigationResponse {
    private boolean success;
    private String message;
    private List<RecommendedMenu> recommendedMenus;
    private String explanation;
    private List<String> alternativeActions;
    private String aiModel;
    private long responseTime;
}