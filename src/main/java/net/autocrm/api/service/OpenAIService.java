package net.autocrm.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OpenAIService {

    private final ChatClient chatClient;
    
    @Value("${autocrm.ai.openai.model:gpt-4o-mini}")
    private String defaultModel;
    
    @Value("${autocrm.ai.openai.max-tokens:1000}")
    private Integer maxTokens;
    
    @Value("${autocrm.ai.openai.temperature:0.7}")
    private Double temperature;

    public OpenAIService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * GPT로 응답 생성
     */
    public Mono<String> generateResponse(String message) {
        return Mono.fromCallable(() -> {
            try {
                log.debug("GPT 요청: {}", message.length() > 100 ? 
                    message.substring(0, 100) + "..." : message);
                
                // AutoCRM 컨텍스트를 위한 시스템 프롬프트
                String systemPrompt = """
                    당신은 AutoCRM 시스템의 AI 어시스턴트입니다.
                    다음 규칙을 따라주세요:
                    1. 한국어로 답변해주세요
                    2. CRM 업무와 관련된 질문에 전문적으로 답변하세요
                    3. 메뉴 검색, 고객 관리, 판매 현황 등에 대해 도움을 제공하세요
                    4. 간결하고 실용적인 답변을 제공하세요
                    5. 확실하지 않은 정보는 추측하지 마세요
                    """;
                
                String fullPrompt = systemPrompt + "\n\n사용자 질문: " + message;
                
                // ChatClient를 사용한 응답 생성 (Spring AI 1.0.0)
                String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(message)
                    .call()
                    .content();
                
                log.debug("GPT 응답 길이: {} 문자", response.length());
                return response;
                
            } catch (Exception e) {
                log.error("GPT API 호출 실패: {}", e.getMessage(), e);
                return "죄송합니다. GPT 서비스에서 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
            }
        })
        .onErrorResume(throwable -> {
            log.error("GPT 응답 생성 중 오류: {}", throwable.getMessage());
            return Mono.just("GPT 서비스 오류: " + throwable.getMessage());
        });
    }

    /**
     * 메뉴 검색 특화 프롬프트
     */
    public Mono<String> searchMenus(String query, String userAuthLevel) {
        String menuPrompt = String.format("""
            AutoCRM 시스템의 메뉴를 검색해주세요.
            사용자 권한: %s
            검색 요청: %s
            
            다음 메뉴들 중에서 관련된 것을 추천해주세요:
            - 고객검색 (/cm/cs/searchCustomer.crm)
            - 판매품의서관리 (/sm/cm/smcm06.do)  
            - 재고차량조회 (/sm/rm/smrm01.do)
            - 판매실적보기 (/sm/om/smom01.do)
            
            JSON 형태로 응답해주세요:
            {
              "menus": [{"name": "메뉴명", "url": "URL", "description": "설명"}],
              "explanation": "추천 이유"
            }
            """, userAuthLevel, query);
        
        return generateResponse(menuPrompt);
    }

    /**
     * 데이터 분석 쿼리 생성
     */
    public Mono<String> generateDataQuery(String request) {
        String queryPrompt = String.format("""
            AutoCRM 데이터베이스 쿼리를 생성해주세요.
            요청: %s
            
            사용 가능한 테이블:
            - SALES_CONFER (판매품의서): CONFER_SEQ, CONTRACT_DATE, DELIVERY_DATE, STATUS, TOTAL_AMOUNT
            - CUSTOMERS (고객): CUSTOMER_SEQ, CUSTOMER_NAME, MOBILE_PHONE
            - SalesUsers (사용자): salesUserSeq, userId, userName, showroomSeq
            
            MSSQL Server 문법으로 작성하고, 다음 형태로 응답해주세요:
            {
              "query": "SELECT ...",
              "explanation": "쿼리 설명",
              "chartType": "pie|bar|line"
            }
            """, request);
        
        return generateResponse(queryPrompt);
    }

    /**
     * API 키 유효성 검사
     */
    public Mono<Boolean> validateApiKey() {
        return generateResponse("테스트")
                .map(response -> !response.contains("API") && !response.contains("오류"))
                .onErrorReturn(false);
    }
}