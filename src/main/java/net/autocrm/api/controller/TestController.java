package net.autocrm.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class TestController {
    
    @GetMapping("/test/token")
    public Map<String, String> getTestToken() {
        // 테스트용 간단한 응답
        return Map.of(
            "message", "AI 테스트는 토큰 없이 접근 가능합니다",
            "ai_test_url", "/api/ai-test",
            "ai_health_url", "/api/ai/health"
        );
    }
}