package com.example.demo;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@SpringBootApplication
@EnableMethodSecurity
public class ResourceServerDemoApplication {
    public static void main(String[] args) { SpringApplication.run(ResourceServerDemoApplication.class, args); }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, CorsConfigurationSource demoCorsConfigurationSource) throws Exception {
        return http.cors(cors -> cors.configurationSource(demoCorsConfigurationSource))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(org.springframework.security.config.Customizer.withDefaults()))
            .build();
    }

    @Bean
    CorsConfigurationSource demoCorsConfigurationSource(@Value("${demo.spa-origin:http://localhost:4173}") String spaOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(spaOrigin));
        configuration.setAllowedMethods(List.of(HttpMethod.GET.name(), HttpMethod.OPTIONS.name()));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        configuration.setExposedHeaders(List.of(HttpHeaders.WWW_AUTHENTICATE));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @RestController
    static class HelloController {
        @GetMapping("/api/hello")
        @PreAuthorize("hasAuthority('SCOPE_profile')")
        String hello() { return "Hello from the protected resource server"; }
    }
}
