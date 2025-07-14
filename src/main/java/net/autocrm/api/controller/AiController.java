package net.autocrm.api.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import net.autocrm.api.model.MenuNavigationResponse;
import net.autocrm.api.model.MenuSearchRequest;
import net.autocrm.api.model.RecommendedMenu;
import net.autocrm.api.service.MultiAIService;
import net.autocrm.api.service.MultiAIService.AIModel;
import net.autocrm.api.service.MultiAIService.AIResponse;
import net.autocrm.api.service.MultiAIService.MultiAIResponse;
import net.autocrm.api.service.MultiDealerMenuNavigationService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ai")
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AiController {
    
    private final ChatClient chatClient;
    private final MultiAIService multiAIService;
    private final MultiDealerMenuNavigationService menuNavigationService;

    // 생성자 주입
    @Autowired(required = false)
    public AiController(ChatClient.Builder chatClientBuilder, 
                       @Autowired(required = false) MultiAIService multiAIService,
                       @Autowired(required = false) MultiDealerMenuNavigationService menuNavigationService) {
        this.chatClient = chatClientBuilder.build();
        this.multiAIService = multiAIService;
        this.menuNavigationService = menuNavigationService;
    }

    /**
     * 기본 ChatClient 사용 (기존 기능 - 딜러에서 호출)
     */
    @PostMapping("")
    @ResponseBody 
    public Map<String, ?> generate(@RequestBody Map<String, String> json) {
        log.info("기본 AI 요청: {}", json.get("userInput"));
        
        HashMap<String, String> rslt = new HashMap<>();
        try {
            String response = this.chatClient.prompt()
                    .advisors(new SimpleLoggerAdvisor())
                    .user(json.get("userInput"))
                    .call()
                    .content();
            
            rslt.put("message", response);
            rslt.put("model", "기본 ChatClient");
            rslt.put("success", "true");
        } catch (Exception e) {
            log.error("기본 AI 응답 오류: {}", e.getMessage());
            rslt.put("message", "AI 응답 중 오류가 발생했습니다: " + e.getMessage());
            rslt.put("success", "false");
        }
        return rslt;
    }

    /**
     * 🆕 통합 AI 처리 - 의도 분석 후 적절한 응답 제공 (딜러 chatbot.js에서 호출)
     */
    @PostMapping("/chat")
    public Mono<ResponseEntity<Map<String, Object>>> integratedChat(@RequestBody IntegratedChatRequest request) {
        log.info("통합 AI 요청: 딜러={}, 권한={}, 사용자={}, 메시지={}", 
                request.getDealerId(), request.getAuthSeq(), request.getUserId(), 
                request.getMessage().length() > 50 ? 
                    request.getMessage().substring(0, 50) + "..." : request.getMessage());

        // 1단계: AI가 사용자 의도 분석
        return analyzeUserIntent(request)
            .flatMap(intent -> {
                log.info("의도 분석 결과: {} (신뢰도: {})", intent.getType(), intent.getConfidence());
                
                // 2단계: 의도에 따른 분기 처리
                return switch (intent.getType()) {
                    case MENU_NAVIGATION -> handleMenuRequest(request, intent);
                    case GENERAL_CHAT -> handleGeneralChat(request, intent);
                    case DATA_ANALYSIS -> handleDataAnalysis(request, intent);
                    case HELP_REQUEST -> handleHelpRequest(request, intent);
                    default -> handleGeneralChat(request, intent);
                };
            })
            .onErrorResume(throwable -> {
                log.error("통합 AI 처리 오류: {}", throwable.getMessage());
                return Mono.just(ResponseEntity.ok(Map.of(
                    "success", false,
                    "type", "error",
                    "message", "AI 처리 중 오류가 발생했습니다: " + throwable.getMessage()
                )));
            });
    }

    /**
     * 1단계: 사용자 의도 분석
     */
    private Mono<UserIntent> analyzeUserIntent(IntegratedChatRequest request) {
        String intentAnalysisPrompt = String.format("""
            다음 사용자 메시지를 분석하여 의도를 파악해주세요.
            
            사용자 메시지: "%s"
            사용자 권한: %s
            딜러: %s
            
            다음 중 하나로 분류해주세요:
            1. MENU_NAVIGATION: 메뉴로 이동하거나 특정 기능을 찾고 싶어하는 경우
               예: "고객 검색하고 싶어", "판매 실적 보는 곳", "재고 관리 어디야", "계약서 관리"
            
            2. GENERAL_CHAT: 일반적인 대화나 질문
               예: "안녕하세요", "오늘 날씨", "CRM이 뭐야", "감사합니다"
            
            3. DATA_ANALYSIS: 데이터 분석이나 통계를 요청하는 경우  
               예: "이번달 매출", "고객 수", "판매 현황", "실적 분석"
            
            4. HELP_REQUEST: 도움말이나 사용법을 묻는 경우
               예: "어떻게 사용해", "기능 설명", "도움말", "사용법"
            
            다음 JSON 형태로만 응답해주세요:
            {
              "type": "MENU_NAVIGATION",
              "confidence": 0.95,
              "keywords": ["키워드1", "키워드2"],
              "reasoning": "분류 근거"
            }
            """, request.getMessage(), request.getAuthSeq(), request.getDealerId());
        
        return multiAIService.getBestResponse(intentAnalysisPrompt, "intent-analysis")
            .map(this::parseUserIntent)
            .onErrorReturn(UserIntent.builder()
                .type(IntentType.GENERAL_CHAT)
                .confidence(0.5)
                .reasoning("의도 분석 실패로 일반 채팅으로 처리")
                .build());
    }
    
    /**
     * 2단계: 메뉴 요청 처리
     */
    private Mono<ResponseEntity<Map<String, Object>>> handleMenuRequest(IntegratedChatRequest request, UserIntent intent) {
        log.info("메뉴 요청 처리: {}", request.getMessage());
        
        MenuSearchRequest menuRequest = new MenuSearchRequest();
        menuRequest.setQuery(request.getMessage());
        menuRequest.setAuthSeq(request.getAuthSeq());
        menuRequest.setDealerId(request.getDealerId());
        menuRequest.setUserId(request.getUserId());
        
        return menuNavigationService.findBestMenu(menuRequest)
            .map(menuResponse -> ResponseEntity.ok(Map.of(
                "success", true,
                "type", "menu_navigation",
                "intent", Map.of(
                    "type", intent.getType(),
                    "confidence", intent.getConfidence(),
                    "reasoning", intent.getReasoning()
                ),
                "data", Map.of(
                    "responseType", "menu",
                    "menuRecommendations", menuResponse.getRecommendedMenus(),
                    "explanation", menuResponse.getExplanation(),
                    "alternativeActions", menuResponse.getAlternativeActions() != null ? 
                        menuResponse.getAlternativeActions() : new ArrayList<>()
                ),
                "message", formatMenuResponse(menuResponse),
                "timestamp", LocalDateTime.now().toString()
            )))
            .onErrorReturn(ResponseEntity.ok(Map.of(
                "success", false,
                "type", "menu_navigation_error",
                "message", "메뉴 검색 중 오류가 발생했습니다."
            )));
    }
    
    /**
     * 일반 채팅 처리
     */
    private Mono<ResponseEntity<Map<String, Object>>> handleGeneralChat(IntegratedChatRequest request, UserIntent intent) {
        log.info("일반 채팅 처리: {}", request.getMessage());
        
        // AutoCRM 컨텍스트를 포함한 일반 채팅 프롬프트
        String chatPrompt = String.format("""
            당신은 AutoCRM 시스템의 AI 어시스턴트입니다.
            사용자와 자연스럽게 대화하면서 AutoCRM 업무에 도움을 주세요.
            
            사용자 권한: %s
            딜러: %s
            사용자 메시지: %s
            
            다음 규칙을 따라주세요:
            1. 친근하고 도움이 되는 톤으로 답변
            2. AutoCRM 관련 질문이면 전문적으로 답변
            3. 일반적인 대화도 자연스럽게 응대
            4. 필요시 메뉴나 기능 안내 제안
            5. 한국어로 답변해주세요
            """, request.getAuthSeq(), request.getDealerId(), request.getMessage());
        
        return multiAIService.getBestResponse(chatPrompt, "general-chat")
            .map(response -> ResponseEntity.ok(Map.of(
                "success", true,
                "type", "general_chat",
                "intent", Map.of(
                    "type", intent.getType(),
                    "confidence", intent.getConfidence(),
                    "reasoning", intent.getReasoning()
                ),
                "data", Map.of(
                    "responseType", "text",
                    "content", response.getContent()
                ),
                "message", response.getContent(),
                "aiModel", response.getModel().getDisplayName(),
                "responseTime", response.getResponseTime(),
                "timestamp", LocalDateTime.now().toString()
            )));
    }
    
    /**
     * 데이터 분석 요청 처리
     */
    private Mono<ResponseEntity<Map<String, Object>>> handleDataAnalysis(IntegratedChatRequest request, UserIntent intent) {
        log.info("데이터 분석 요청 처리: {}", request.getMessage());
        
        String analysisPrompt = String.format("""
            사용자가 다음과 같은 데이터 분석을 요청했습니다:
            "%s"
            
            현재는 실제 데이터베이스 연결이 되어있지 않으므로,
            어떤 종류의 분석이 가능한지 안내하고
            해당 데이터를 볼 수 있는 메뉴를 추천해주세요.
            
            권한: %s
            딜러: %s
            
            친근하고 도움이 되는 톤으로 답변해주세요.
            """, request.getMessage(), request.getAuthSeq(), request.getDealerId());
        
        return multiAIService.getBestResponse(analysisPrompt, "data-analysis")
            .map(response -> ResponseEntity.ok(Map.of(
                "success", true,
                "type", "data_analysis",
                "intent", Map.of(
                    "type", intent.getType(),
                    "confidence", intent.getConfidence(),
                    "reasoning", intent.getReasoning()
                ),
                "data", Map.of(
                    "responseType", "analysis_guide",
                    "content", response.getContent(),
                    "suggestedMenus", Arrays.asList("판매실적보기", "통계자료", "실적관리")
                ),
                "message", response.getContent(),
                "timestamp", LocalDateTime.now().toString()
            )));
    }
    
    /**
     * 도움말 요청 처리
     */
    private Mono<ResponseEntity<Map<String, Object>>> handleHelpRequest(IntegratedChatRequest request, UserIntent intent) {
        log.info("도움말 요청 처리: {}", request.getMessage());
        
        String helpContent = String.format("""
            🤖 **AutoCRM AI 도우미 사용법**
            
            **💬 메뉴 안내**
            - "고객 검색하고 싶어" → 고객검색 메뉴로 안내
            - "판매 실적 보여줘" → 판매실적 메뉴로 안내
            - "재고 관리 어디야" → 재고관리 메뉴로 안내
            
            **📊 데이터 문의**
            - "이번달 매출은?" → 해당 데이터 메뉴 추천
            - "고객 수 알려줘" → 통계 메뉴 안내
            
            **💡 일반 대화**
            - AutoCRM 관련 질문이나 일반적인 대화 가능
            
            **⌨️ 단축키**
            - Shift + F1: 채팅창 열기/닫기
            
            현재 권한: %s | 딜러: %s
            """, request.getAuthSeq(), request.getDealerId());
        
        return Mono.just(ResponseEntity.ok(Map.of(
            "success", true,
            "type", "help",
            "intent", Map.of(
                "type", intent.getType(),
                "confidence", intent.getConfidence(),
                "reasoning", intent.getReasoning()
            ),
            "data", Map.of(
                "responseType", "help",
                "content", helpContent
            ),
            "message", helpContent,
            "timestamp", LocalDateTime.now().toString()
        )));
    }

    // === 기존 메서드들 (그대로 유지) ===

    /**
     * 멀티 AI 채팅 (기존 기능)
     */
    @PostMapping("/multi-chat")
    public Mono<ResponseEntity<Map<String, Object>>> multiChat(@RequestBody ChatRequest request) {
        log.info("멀티 AI 채팅 요청: 모델={}, 사용자={}, 메시지={}", 
                request.getModel(), request.getUserId(), 
                request.getMessage().length() > 50 ? 
                    request.getMessage().substring(0, 50) + "..." : request.getMessage());

        AIModel model = parseModel(request.getModel());
        
        return multiAIService.getChatResponse(request.getMessage(), model, request.getUserId())
                .map(response -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                        "content", response.getContent(),
                        "model", response.getModel().getDisplayName(),
                        "responseTime", response.getResponseTime() + "ms",
                        "confidence", String.format("%.1f%%", response.getConfidence() * 100),
                        "cost", response.getCost().toString()
                    ),
                    "timestamp", response.getTimestamp().toString()
                )))
                .onErrorResume(throwable -> {
                    log.error("멀티 AI 채팅 오류: {}", throwable.getMessage());
                    return Mono.just(ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "AI 응답 중 오류가 발생했습니다: " + throwable.getMessage()
                    )));
                });
    }

    /**
     * 모든 모델 비교 응답 (기존 기능)
     */
    @PostMapping("/compare")
    public Mono<ResponseEntity<Map<String, Object>>> compareModels(@RequestBody ChatRequest request) {
        log.info("AI 모델 비교 요청: 사용자={}, 메시지={}", 
                request.getUserId(), 
                request.getMessage().length() > 50 ? 
                    request.getMessage().substring(0, 50) + "..." : request.getMessage());

        return multiAIService.getAllModelResponses(request.getMessage(), request.getUserId())
                .map(this::formatComparisonResponse)
                .onErrorResume(throwable -> {
                    log.error("AI 비교 응답 오류: {}", throwable.getMessage());
                    return Mono.just(ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "AI 모델 비교 중 오류가 발생했습니다: " + throwable.getMessage()
                    )));
                });
    }

    /**
     * 최적 모델 자동 선택 (기존 기능)
     */
    @PostMapping("/best")
    public Mono<ResponseEntity<Map<String, Object>>> getBestResponse(@RequestBody ChatRequest request) {
        log.info("AI 최적 모델 요청: 사용자={}, 메시지={}", 
                request.getUserId(), 
                request.getMessage().length() > 50 ? 
                    request.getMessage().substring(0, 50) + "..." : request.getMessage());

        return multiAIService.getBestResponse(request.getMessage(), request.getUserId())
                .map(response -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                        "content", response.getContent(),
                        "selectedModel", response.getModel().getDisplayName(),
                        "responseTime", response.getResponseTime() + "ms",
                        "confidence", String.format("%.1f%%", response.getConfidence() * 100),
                        "reason", "메시지 길이와 복잡도를 고려하여 선택됨"
                    ),
                    "timestamp", response.getTimestamp().toString()
                )))
                .onErrorResume(throwable -> {
                    log.error("AI 최적 응답 오류: {}", throwable.getMessage());
                    return Mono.just(ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "AI 최적 응답 중 오류가 발생했습니다: " + throwable.getMessage()
                    )));
                });
    }

    /**
     * 사용 가능한 AI 모델 목록 (기존 기능)
     */
    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> getAvailableModels() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", Map.of(
                "models", java.util.Arrays.stream(AIModel.values())
                    .map(model -> Map.of(
                        "id", model.name().toLowerCase(),
                        "name", model.getDisplayName(),
                        "type", model.getType().name(),
                        "description", getModelDescription(model)
                    ))
                    .toList()
            )
        ));
    }

    /**
     * 헬스체크 (기존 기능)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "AI 서비스가 정상 작동 중입니다",
            "timestamp", java.time.LocalDateTime.now().toString(),
            "chatClient", "활성화",
            "multiAIService", multiAIService != null ? "활성화" : "비활성화",
            "menuNavigationService", menuNavigationService != null ? "활성화" : "비활성화",
            "availableEndpoints", java.util.List.of(
                "POST /ai (기본 ChatClient - 딜러 호출용)",
                "POST /ai/chat (통합 AI - 딜러 chatbot 호출용)",
                "POST /ai/multi-chat (멀티 AI 채팅)",
                "POST /ai/compare (모델 비교)",
                "POST /ai/best (최적 모델)",
                "GET /ai/models (모델 목록)",
                "GET /ai/health (상태 확인)"
            )
        ));
    }

    // === 유틸리티 메서드들 ===

    private AIModel parseModel(String modelStr) {
        if (modelStr == null || modelStr.trim().isEmpty()) {
            return AIModel.LINKBRICKS;
        }
        
        return switch (modelStr.toLowerCase()) {
            case "linkbricks", "link", "fast" -> AIModel.LINKBRICKS;
            case "eeve", "accurate", "quality" -> AIModel.EEVE;
            case "llama32", "llama3.2", "legacy" -> AIModel.LLAMA32;
            case "gpt", "openai", "premium" -> AIModel.GPT_4O_MINI;
            default -> AIModel.LINKBRICKS;
        };
    }

    /**
     * 사용자 의도 파싱
     */
    private UserIntent parseUserIntent(AIResponse aiResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(aiResponse.getContent());
            
            return UserIntent.builder()
                .type(IntentType.valueOf(jsonResponse.get("type").asText()))
                .confidence(jsonResponse.get("confidence").asDouble())
                .keywords(parseStringArray(jsonResponse.get("keywords")))
                .reasoning(jsonResponse.get("reasoning").asText())
                .build();
                
        } catch (Exception e) {
            log.warn("의도 파싱 실패, 일반 채팅으로 처리: {}", e.getMessage());
            return UserIntent.builder()
                .type(IntentType.GENERAL_CHAT)
                .confidence(0.5)
                .reasoning("파싱 실패")
                .build();
        }
    }
    
    /**
     * 메뉴 응답 포맷팅
     */
    private String formatMenuResponse(MenuNavigationResponse menuResponse) {
        if (!menuResponse.isSuccess() || menuResponse.getRecommendedMenus() == null || 
            menuResponse.getRecommendedMenus().isEmpty()) {
            return "죄송합니다. 해당하는 메뉴를 찾을 수 없습니다.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("🎯 ").append(menuResponse.getExplanation() != null ? 
            menuResponse.getExplanation() : "메뉴를 찾았습니다.").append("\n\n");
        
        for (RecommendedMenu menu : menuResponse.getRecommendedMenus()) {
            sb.append("📋 **").append(menu.getName()).append("**\n");
            sb.append("📂 ").append(menu.getCategory()).append("\n");
            sb.append("💡 ").append(menu.getReason()).append("\n");
            sb.append("📊 신뢰도: ").append(Math.round(menu.getConfidence() * 100)).append("%\n\n");
        }
        
        return sb.toString();
    }

    private ResponseEntity<Map<String, Object>> formatComparisonResponse(MultiAIResponse multiResponse) {
        Map<String, Object> responseData = new java.util.HashMap<>();
        
        Map<String, Object> modelResponses = new java.util.HashMap<>();
        multiResponse.getResponses().forEach((model, response) -> {
            modelResponses.put(model.name().toLowerCase(), Map.of(
                "content", response.getContent(),
                "responseTime", response.getResponseTime() + "ms",
                "confidence", String.format("%.1f%%", response.getConfidence() * 100),
                "cost", response.getCost().toString(),
                "error", response.isError()
            ));
        });
        
        Map<String, Object> comparison = Map.of(
            "fastest", multiResponse.getComparison().getFastestModel().getDisplayName(),
            "mostAccurate", multiResponse.getComparison().getMostAccurateModel().getDisplayName(),
            "mostCostEffective", multiResponse.getComparison().getMostCostEffective().getDisplayName(),
            "recommended", multiResponse.getComparison().getRecommendedModel().getDisplayName()
        );
        
        responseData.put("success", true);
        responseData.put("data", Map.of(
            "question", multiResponse.getQuestion(),
            "responses", modelResponses,
            "comparison", comparison,
            "totalTime", multiResponse.getTotalProcessingTime() + "ms"
        ));
        responseData.put("timestamp", multiResponse.getTimestamp().toString());
        
        return ResponseEntity.ok(responseData);
    }

    private String getModelDescription(AIModel model) {
        return switch (model) {
            case LINKBRICKS -> "빠른 응답 속도의 한글 특화 모델 (3-6초)";
            case EEVE -> "높은 품질의 한글 모델 (8-15초)";
            case LLAMA32 -> "기존 호환성을 위한 Llama3.2 한글 모델 (5-10초)";
            case GPT_4O_MINI -> "OpenAI의 정확하고 안정적인 모델 (1-3초, 유료)";
        };
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

    // === DTO 클래스들 ===

    /**
     * 기존 요청 DTO (멀티 AI 용)
     */
    public static class ChatRequest {
        private String message;
        private String model;
        private String userId;
        private String sessionId;

        // Getters and Setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }

    /**
     * 🆕 통합 AI 요청 DTO (딜러 chatbot용)
     */
    public static class IntegratedChatRequest {
        private String message;
        private String authSeq;
        private String dealerId;
        private String userId;
        private String sessionId;

        // Getters and Setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getAuthSeq() { return authSeq; }
        public void setAuthSeq(String authSeq) { this.authSeq = authSeq; }
        
        public String getDealerId() { return dealerId; }
        public void setDealerId(String dealerId) { this.dealerId = dealerId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }

    /**
     * 사용자 의도 모델
     */
    @lombok.Data
    @lombok.Builder
    public static class UserIntent {
        private IntentType type;
        private double confidence;
        private List<String> keywords;
        private String reasoning;
    }

    /**
     * 의도 타입
     */
    public enum IntentType {
        MENU_NAVIGATION,  // 메뉴 이동 요청
        GENERAL_CHAT,     // 일반 대화
        DATA_ANALYSIS,    // 데이터 분석 요청
        HELP_REQUEST      // 도움말 요청
    }
}