package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${notificaciones.url.base:http://localhost:5173}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Orígenes permitidos (desarrollo + producción)
        configuration.setAllowedOriginPatterns(Arrays.asList(
                frontendUrl,
                "https://*.vercel.app",
                "https://*.onrender.com",
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Headers permitidos
        configuration.setAllowedHeaders(List.of("*"));

        // Permitir credenciales
        configuration.setAllowCredentials(true);

        // Headers expuestos
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", 
                "Content-Type",
                "X-Total-Count"
        ));

        // Tiempo de cache para preflight
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
