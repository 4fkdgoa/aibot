package net.autocrm.api.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@ConfigurationProperties(prefix = "autocrm.dealer")
@Configuration
@Data
public class DealerConfigProperties {
    
    private Map<String, DealerConfig> configs = new HashMap<>();
    private String defaultDealer = "SCL";
    
    @Data
    public static class DealerConfig {
        private String webappPath;
        private String projectName;
        private String contextPath;
        private String dbName;
        private String displayName;
        private boolean enabled = true;
    }
    
    /**
     * 딜러 설정 조회
     */
    public DealerConfig getDealerConfig(String dealerId) {
        return configs.get(dealerId);
    }
    
    /**
     * 모든 활성화된 딜러 목록
     */
    public Set<String> getActiveDealers() {
        return configs.entrySet().stream()
            .filter(entry -> entry.getValue().isEnabled())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }
    
    /**
     * leftmenu.jsp 경로 생성
     */
    public String getLeftMenuPath(String dealerId) {
        DealerConfig config = getDealerConfig(dealerId);
        if (config == null) {
            throw new IllegalArgumentException("딜러 설정을 찾을 수 없습니다: " + dealerId);
        }
        
        return config.getWebappPath() + "/WEB-INF/jsp/layout/leftmenu.jsp";
    }
}