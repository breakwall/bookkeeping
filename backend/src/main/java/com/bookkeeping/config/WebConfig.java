package com.bookkeeping.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    /**
     * CORS 允许的来源
     * 支持多个来源，用逗号分隔
     * 默认值：http://localhost:3000（开发环境）
     * 生产环境建议通过环境变量 CORS_ALLOWED_ORIGINS 配置
     */
    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 将逗号分隔的字符串转换为数组
        String[] origins = allowedOrigins.split(",");
        
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
