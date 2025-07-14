package net.autocrm.api.service;


import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import net.autocrm.api.model.MenuInfo;
import net.autocrm.api.model.MenuNavigationResponse;
import net.autocrm.api.model.MenuSearchRequest;
import net.autocrm.api.model.RecommendedMenu;
import net.autocrm.api.service.MultiAIService.AIResponse;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class MultiDealerMenuNavigationService {
    
    private final MultiDealerMenuParsingService menuParsingService;
    private final MultiAIService multiAIService;
    private final DealerConfigProperties dealerConfig;
    
    /**
     * 딜러별 AI 메뉴 검색
     */
    public Mono<MenuNavigationResponse> findBestMenu(MenuSearchRequest request) {
        String userQuery = request.getQuery();
        String authSeq = request.getAuthSeq();
        String dealerId = request.getDealerId();
        
        log.info("딜러별 메뉴 검색: 딜러={}, 권한={}, 쿼리={}", dealerId, authSeq, userQuery);
        
        // 1. 딜러 설정 확인
        if (!dealerConfig.getActiveDealers().contains(dealerId)) {
            return Mono.just(MenuNavigationResponse.builder()
                .success(false)
                .message("지원되지 않는 딜러입니다: " + dealerId)
                .build());
        }
        
        // 2. 해당 딜러의 권한별 메뉴 목록 가져오기
        List<MenuInfo> availableMenus = menuParsingService.getMenusByDealerAndAuth(dealerId, authSeq);
        
        if (availableMenus.isEmpty()) {
            return Mono.just(MenuNavigationResponse.builder()
                .success(false)
                .message(String.format("딜러 %s의 권한 %s로 사용할 수 있는 메뉴가 없습니다.", dealerId, authSeq))
                .build());
        }
        
        // 3. AI에게 메뉴 매칭 요청
        String prompt = createDealerSpecificPrompt(userQuery, availableMenus, authSeq, dealerId);
        
        return multiAIService.getBestResponse(prompt, "dealer-menu-navigation")
            .map(this::parseMenuResponse)
            .onErrorResume(throwable -> {
                log.error("딜러 {} AI 메뉴 검색 오류: {}", dealerId, throwable.getMessage());
                return Mono.just(MenuNavigationResponse.builder()
                    .success(false)
                    .message("메뉴 검색 중 오류가 발생했습니다: " + throwable.getMessage())
                    .build());
            });
    }
    
    /**
     * 딜러별 특화 프롬프트 생성
     */
    private String createDealerSpecificPrompt(String userQuery, List<MenuInfo> availableMenus, 
                                            String authSeq, String dealerId) {
        DealerConfigProperties.DealerConfig config = dealerConfig.getDealerConfig(dealerId);
        String dealerName = config.getDisplayName() != null ? config.getDisplayName() : dealerId;
        
        StringBuilder menuList = new StringBuilder();
        
        for (MenuInfo mainMenu : availableMenus) {
            menuList.append("## ").append(mainMenu.getMenuName()).append("\n");
            menuList.append("\n");
        }
        
        return String.format("""
            당신은 %s 딜러의 AutoCRM 시스템 메뉴 안내 전문가입니다.
            딜러 코드: %s
            사용자 권한: %s
            
            사용자 요청: "%s"
            
            %s 딜러의 사용 가능한 메뉴 목록:
            %s
            
            다음 규칙을 따라 메뉴를 추천해주세요:
            1. %s 딜러에 특화된 메뉴 구조를 고려해주세요
            2. 사용자 권한 %s에 맞는 메뉴만 추천해주세요
            3. 정확한 URL 경로를 제공해주세요
            
            다음 JSON 형태로만 응답해주세요:
            {
              "found": true,
              "dealerId": "%s",
              "recommendedMenus": [
                {
                  "name": "메뉴명",
                  "url": "URL경로", 
                  "category": "상위메뉴명",
                  "confidence": 0.95,
                  "reason": "이 메뉴가 적합한 이유"
                }
              ],
              "explanation": "전체적인 설명과 사용 방법",
              "alternativeActions": ["대안 제안1", "대안 제안2"]
            }
            """, dealerName, dealerId, authSeq, userQuery, dealerName, menuList.toString(), 
                 dealerName, authSeq, dealerId);
    }
    
    /**
     * AI 응답을 파싱하여 MenuNavigationResponse 생성
     */
    private MenuNavigationResponse parseMenuResponse(AIResponse aiResponse) {
        try {
            String content = aiResponse.getContent();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(content);
            
            List<RecommendedMenu> recommendedMenus = new ArrayList<>();
            JsonNode menusNode = jsonResponse.get("recommendedMenus");
            
            if (menusNode != null && menusNode.isArray()) {
                for (JsonNode menuNode : menusNode) {
                    RecommendedMenu menu = RecommendedMenu.builder()
                        .name(getJsonText(menuNode, "name"))
                        .url(getJsonText(menuNode, "url"))
                        .category(getJsonText(menuNode, "category"))
                        .confidence(getJsonDouble(menuNode, "confidence"))
                        .reason(getJsonText(menuNode, "reason"))
                        .build();
                    recommendedMenus.add(menu);
                }
            }
            
            return MenuNavigationResponse.builder()
                .success(getJsonBoolean(jsonResponse, "found"))
                .recommendedMenus(recommendedMenus)
                .explanation(getJsonText(jsonResponse, "explanation"))
                .alternativeActions(parseStringArray(jsonResponse.get("alternativeActions")))
                .aiModel(aiResponse.getModel().getDisplayName())
                .responseTime(aiResponse.getResponseTime())
                .build();
                
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패: {}", e.getMessage());
            
            // 파싱 실패시 간단한 텍스트 응답 제공
            return MenuNavigationResponse.builder()
                .success(true)
                .explanation(aiResponse.getContent())
                .build();
        }
    }
    
    // JSON 파싱 헬퍼 메서드들
    private String getJsonText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asText() : "";
    }
    
    private double getJsonDouble(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asDouble() : 0.0;
    }
    
    private boolean getJsonBoolean(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asBoolean() : false;
    }
    
    private List<String> parseStringArray(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                result.add(item.asText());
            }
        }
        return result;
    }
    
    /**
     * 권한별 전체 메뉴 목록 반환
     */
    public List<MenuInfo> getMenusByAuth(String authSeq) {
        return menuParsingService.getMenusByAuth(authSeq);
    }
    
    /**
     * 파싱 상태 정보
     */
    public Map<String, Object> getParsingStatus() {
        return Map.of(
            "parsedAuthorities", menuParsingService.getParsedAuthorities(),
            "lastParsed", menuParsingService.getLastParseTime(),
            "totalMenus", menuParsingService.getTotalMenuCount()
        );
    }
}




