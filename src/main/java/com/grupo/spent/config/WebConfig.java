package com.grupo.spent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("https://spent-alpha.vercel.app") // Asegúrate de que no hay barra inclinada al final. http://localhost:5173 for testing
                        .allowedMethods("GET", "POST", "DELETE", "PATCH", "PUT")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
