package tn.vermeg.gestionproduit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
@Configuration
public class CorsConfig {

    @Value("${web.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;
    @Value("${web.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;
    @Value("${web.cors.allowed-headers:*}")
    private String allowedHeaders;
    @Value("${web.cors.allow-credentials:true}")
    private boolean allowCredentials;
    @Value("${web.cors.max-age:3600}")
    private long maxAge;
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(allowCredentials);
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        config.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        config.setMaxAge(maxAge);
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
