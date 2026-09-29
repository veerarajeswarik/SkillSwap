package com.example.backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * One central CORS rule for the whole API (instead of @CrossOrigin on every controller).
 *
 * LOCAL DEVELOPMENT ONLY. Allowed browser origins:
 *   http://localhost:<any port>    e.g. VS Code Live Server on http://localhost:5500
 *   http://127.0.0.1:<any port>    e.g. http://127.0.0.1:5500
 *   null                           what browsers send for a page opened by double-click (file:///)
 *
 * PRODUCTION: replace these with the real frontend address, e.g. "https://skillswap.example.com".
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:[*]", "http://127.0.0.1:[*]", "null")
                .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept");
    }
}
