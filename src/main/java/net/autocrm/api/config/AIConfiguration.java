package net.autocrm.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AIConfiguration {

//    /**
//     * ChatClient 빌더 (Spring Boot Auto Configuration 사용)
//     */
//    @Bean
//    public ChatClient.Builder chatClientBuilder(OpenAiChatModel chatModel) {
//        return ChatClient.builder(chatModel);
//    }

    /**
     * WebClient 빌더 (Ollama 통신용)
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)); // 2MB
    }
}