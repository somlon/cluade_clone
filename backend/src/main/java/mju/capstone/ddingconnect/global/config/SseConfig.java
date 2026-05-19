package mju.capstone.ddingconnect.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SseConfig implements WebMvcConfigurer {
    //sse 세션 유지시간 -> 무한
    private static final long SSE_ASYNC_TIMEOUT = -1L;

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(SSE_ASYNC_TIMEOUT);
    }
}
