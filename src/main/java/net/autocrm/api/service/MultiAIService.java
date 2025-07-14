package net.autocrm.api.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MultiAIService {

    @Value("${autocrm.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${autocrm.ai.openai.api-key:}")
    private String openaiApiKey;

    private final WebClient webClient;
    private final OpenAIService openAIService;

    public MultiAIService(WebClient.Builder webClientBuilder, OpenAIService openAIService) {
        this.webClient = webClientBuilder
            .baseUrl("http://localhost:11434") // Ollama 기본 URL
            .build();
        this.openAIService = openAIService;
    }

    /**
     * AI 모델 열거형
     */
    public enum AIModel {
        LINKBRICKS("benedict/linkbricks-llama3.1-korean:8b", "LinkBricks 한글 8B", ModelType.OLLAMA),
        EEVE("hf.co/heegyu/EEVE-Korean-Instruct-10.8B-v1.0-GGUF", "EEVE 한글 10.8B", ModelType.OLLAMA),
        LLAMA32("llama3.2-korean", "Llama3.2 한글 (기존)", ModelType.OLLAMA),
        GPT_4O_MINI("gpt-4o-mini", "GPT-4o-mini", ModelType.OPENAI);

        private final String modelName;
        private final String displayName;
        private final ModelType type;

        AIModel(String modelName, String displayName, ModelType type) {
            this.modelName = modelName;
            this.displayName = displayName;
            this.type = type;
        }

        public String getModelName() { return modelName; }
        public String getDisplayName() { return displayName; }
        public ModelType getType() { return type; }
    }

    public enum ModelType {
        OLLAMA, OPENAI
    }

    /**
     * 단일 모델 응답 요청
     */
    public Mono<AIResponse> getChatResponse(String message, AIModel model, String userId) {
        long startTime = System.currentTimeMillis();
        
        return switch (model.getType()) {
            case OLLAMA -> getOllamaResponse(message, model)
                    .map(content -> createAIResponse(content, model, startTime));
            case OPENAI -> getOpenAIResponse(message, model)
                    .map(content -> createAIResponse(content, model, startTime));
        };
    }

    /**
     * 모든 모델 동시 응답 (비교용)
     */
    public Mono<MultiAIResponse> getAllModelResponses(String message, String userId) {
        long startTime = System.currentTimeMillis();
        
        // 병렬로 모든 모델 호출
        CompletableFuture<AIResponse> linkbricksTask = 
            getChatResponse(message, AIModel.LINKBRICKS, userId).toFuture();
        CompletableFuture<AIResponse> eeveTask = 
            getChatResponse(message, AIModel.EEVE, userId).toFuture();
        CompletableFuture<AIResponse> llama32Task = 
            getChatResponse(message, AIModel.LLAMA32, userId).toFuture();
        CompletableFuture<AIResponse> gptTask = 
            getChatResponse(message, AIModel.GPT_4O_MINI, userId).toFuture();

        return Mono.fromFuture(
            CompletableFuture.allOf(linkbricksTask, eeveTask, llama32Task, gptTask)
                .thenApply(v -> {
                    Map<AIModel, AIResponse> responses = new HashMap<>();
                    
                    try {
                        responses.put(AIModel.LINKBRICKS, linkbricksTask.get(1, TimeUnit.SECONDS));
                    } catch (Exception e) {
                        log.warn("LinkBricks 응답 실패: {}", e.getMessage());
                        responses.put(AIModel.LINKBRICKS, createErrorResponse(AIModel.LINKBRICKS, e.getMessage()));
                    }
                    
                    try {
                        responses.put(AIModel.EEVE, eeveTask.get(5, TimeUnit.SECONDS));
                    } catch (Exception e) {
                        log.warn("EEVE 응답 실패: {}", e.getMessage());
                        responses.put(AIModel.EEVE, createErrorResponse(AIModel.EEVE, e.getMessage()));
                    }
                    
                    try {
                        responses.put(AIModel.LLAMA32, llama32Task.get(3, TimeUnit.SECONDS));
                    } catch (Exception e) {
                        log.warn("Llama3.2 응답 실패: {}", e.getMessage());
                        responses.put(AIModel.LLAMA32, createErrorResponse(AIModel.LLAMA32, e.getMessage()));
                    }
                    
                    try {
                        responses.put(AIModel.GPT_4O_MINI, gptTask.get(3, TimeUnit.SECONDS));
                    } catch (Exception e) {
                        log.warn("GPT 응답 실패: {}", e.getMessage());
                        responses.put(AIModel.GPT_4O_MINI, createErrorResponse(AIModel.GPT_4O_MINI, e.getMessage()));
                    }

                    return MultiAIResponse.builder()
                            .question(message)
                            .responses(responses)
                            .comparison(analyzeResponses(responses))
                            .totalProcessingTime(System.currentTimeMillis() - startTime)
                            .userId(userId)
                            .timestamp(LocalDateTime.now())
                            .build();
                })
        );
    }

    /**
     * 최적 모델 선택 응답
     */
    public Mono<AIResponse> getBestResponse(String message, String userId) {
        // 메시지 길이와 복잡도에 따라 모델 선택
        AIModel selectedModel = selectOptimalModel(message);
        
        log.info("선택된 모델: {} (메시지: '{}')", selectedModel.getDisplayName(), 
                message.length() > 50 ? message.substring(0, 50) + "..." : message);
        
        return getChatResponse(message, selectedModel, userId);
    }

    /**
     * Ollama 응답 처리
     */
    private Mono<String> getOllamaResponse(String message, AIModel model) {
        Map<String, Object> requestBody = Map.of(
            "model", model.getModelName(),
            "prompt", message,
            "stream", false,
            "options", Map.of(
                "temperature", 0.7,
                "num_predict", 1000
            )
        );

        return webClient.post()
                .uri(ollamaBaseUrl + "/api/generate")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("response"))
                .onErrorResume(throwable -> {
                    log.error("Ollama API 호출 실패: {}", throwable.getMessage());
                    return Mono.just("죄송합니다. " + model.getDisplayName() + " 모델에서 오류가 발생했습니다.");
                });
    }

    /**
     * OpenAI 응답 처리
     */
    private Mono<String> getOpenAIResponse(String message, AIModel model) {
        return openAIService.generateResponse(message)
                .onErrorResume(throwable -> {
                    log.error("OpenAI API 호출 실패: {}", throwable.getMessage());
                    return Mono.just("죄송합니다. GPT 모델에서 오류가 발생했습니다: " + throwable.getMessage());
                });
    }

    /**
     * 최적 모델 선택 로직
     */
    private AIModel selectOptimalModel(String message) {
        int messageLength = message.length();
        
        // 간단한 질문 (50자 이하) -> 빠른 모델
        if (messageLength <= 50) {
            return AIModel.LINKBRICKS;
        }
        // 복잡한 질문 (200자 이상) -> 정확한 모델  
        else if (messageLength >= 200) {
            return AIModel.EEVE;
        }
        // 중간 길이 -> 균형 모델
        else {
            return AIModel.LINKBRICKS;
        }
    }

    /**
     * 응답 분석 및 비교
     */
    private ComparisonResult analyzeResponses(Map<AIModel, AIResponse> responses) {
        AIModel fastestModel = responses.entrySet().stream()
                .filter(entry -> !entry.getValue().isError())
                .min(Map.Entry.comparingByValue((r1, r2) -> 
                    Long.compare(r1.getResponseTime(), r2.getResponseTime())))
                .map(Map.Entry::getKey)
                .orElse(AIModel.LINKBRICKS);

        AIModel recommendedModel = fastestModel; // 기본적으로 가장 빠른 모델 추천
        
        return ComparisonResult.builder()
                .fastestModel(fastestModel)
                .mostAccurateModel(AIModel.EEVE) // EEVE가 일반적으로 더 정확
                .mostCostEffective(AIModel.LINKBRICKS) // Ollama 모델들은 무료
                .recommendedModel(recommendedModel)
                .build();
    }

    /**
     * AI 응답 객체 생성
     */
    private AIResponse createAIResponse(String content, AIModel model, long startTime) {
        long responseTime = System.currentTimeMillis() - startTime;
        
        return AIResponse.builder()
                .content(content)
                .model(model)
                .responseTime(responseTime)
                .confidence(calculateConfidence(content, model))
                .cost(calculateCost(model, content.length()))
                .timestamp(LocalDateTime.now())
                .isError(false)
                .build();
    }

    /**
     * 에러 응답 생성
     */
    private AIResponse createErrorResponse(AIModel model, String errorMessage) {
        return AIResponse.builder()
                .content("오류: " + errorMessage)
                .model(model)
                .responseTime(0L)
                .confidence(0.0)
                .cost(BigDecimal.ZERO)
                .timestamp(LocalDateTime.now())
                .isError(true)
                .build();
    }

    /**
     * 신뢰도 계산 (간단한 휴리스틱)
     */
    private double calculateConfidence(String content, AIModel model) {
        if (content == null || content.trim().isEmpty()) {
            return 0.0;
        }
        
        // 모델별 기본 신뢰도
        double baseConfidence = switch (model) {
            case EEVE -> 0.90;          // 큰 모델, 높은 신뢰도
            case LINKBRICKS -> 0.85;    // 중간 크기, 좋은 성능
            case LLAMA32 -> 0.80;       // 기존 모델, 호환성
            case GPT_4O_MINI -> 0.95;   // GPT, 가장 높은 신뢰도
        };
        
        // 응답 길이에 따른 조정
        int contentLength = content.length();
        if (contentLength < 10) baseConfidence *= 0.5;
        else if (contentLength > 100) baseConfidence *= 1.1;
        
        return Math.min(baseConfidence, 1.0);
    }

    /**
     * 비용 계산
     */
    private BigDecimal calculateCost(AIModel model, int contentLength) {
        return switch (model.getType()) {
            case OLLAMA -> BigDecimal.ZERO; // Ollama는 무료
            case OPENAI -> BigDecimal.valueOf(contentLength * 0.00001); // 대략적인 GPT 비용
        };
    }

    /**
     * 응답 데이터 클래스들
     */
    @Data
    @lombok.Builder
    public static class MultiAIResponse {
        private String question;
        private Map<AIModel, AIResponse> responses;
        private ComparisonResult comparison;
        private long totalProcessingTime;
        private String userId;
        private LocalDateTime timestamp;
    }

    @Data
    @lombok.Builder
    public static class AIResponse {
        private String content;
        private AIModel model;
        private long responseTime;
        private double confidence;
        private BigDecimal cost;
        private LocalDateTime timestamp;
        private boolean isError;
        private Map<String, Object> metadata;
    }

    @Data
    @lombok.Builder
    public static class ComparisonResult {
        private AIModel fastestModel;
        private AIModel mostAccurateModel;
        private AIModel mostCostEffective;
        private AIModel recommendedModel;
        private String explanation;
    }
}