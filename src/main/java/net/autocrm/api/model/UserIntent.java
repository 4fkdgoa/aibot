package net.autocrm.api.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * 사용자 의도 모델
 */
@Data
@Builder
public class UserIntent {
    private IntentType type;
    private double confidence;
    private List<String> keywords;
    private String reasoning;
}

