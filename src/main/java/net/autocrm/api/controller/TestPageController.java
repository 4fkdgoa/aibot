package net.autocrm.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TestPageController {

    // Thymeleaf 템플릿 방식
    @GetMapping("/ai-test")
    public String aiTestPage() {
        return "ai-test"; // templates/ai-test.html
    }
    
    // 간단한 텍스트 응답으로 테스트
    @GetMapping("/ai-simple")
    @ResponseBody
    public String simpleTest() {
        return "<h1>AI 테스트 페이지 연결 성공!</h1><p>이제 /ai-test로 이동하세요.</p>";
    }
    
    @GetMapping("/test")
    public String testPage() {
        return "test"; // 간단한 테스트 페이지
    }    
}