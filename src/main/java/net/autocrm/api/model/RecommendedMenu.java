package net.autocrm.api.model;

import lombok.Builder;
import lombok.Data;

@Data 
@Builder
public class RecommendedMenu {
    private String name;
    private String url;
    private String category;
    private double confidence;
    private String reason;
}