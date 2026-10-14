package com.example.authserver.config;

import com.example.authserver.service.authorization.RefreshTokenFamilyService;
import com.example.authserver.service.authorization.RefreshTokenReuseAuthenticationProvider;
import com.example.authserver.security.AdminBootstrapTokenFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    @Bean @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,CorsConfigurationSource oauthCorsConfigurationSource,
        OAuth2AuthorizationService authorizations,OAuth2TokenGenerator<?> tokenGenerator,RefreshTokenFamilyService families) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        var configurer=http.getConfigurer(org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer.class);
        configurer.oidc(Customizer.withDefaults())
            .authorizationEndpoint(endpoint -> endpoint.consentPage("/oauth2/consent"))
            .tokenEndpoint(endpoint -> endpoint.authenticationProviders(providers -> {
                providers.removeIf(provider -> provider instanceof org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationProvider);
                providers.add(0,new RefreshTokenReuseAuthenticationProvider(families,authorizations,tokenGenerator));
                OAuth2AuthorizationCodeRequestAuthenticationValidator defaults=new OAuth2AuthorizationCodeRequestAuthenticationValidator();
                providers.stream().filter(provider->provider instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider)
                    .map(provider->(OAuth2AuthorizationCodeRequestAuthenticationProvider)provider)
                    .forEach(provider->provider.setAuthenticationValidator(context->{
                        defaults.accept(context);
                        Object method=context.getAuthorizationRequest().getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE_METHOD);
                        if(!"S256".equals(method))throw new OAuth2AuthenticationException(new OAuth2Error("invalid_request","PKCE S256 is required",null));
                    }));
            }));
        http.cors(cors -> cors.configurationSource(oauthCorsConfigurationSource));
        http.exceptionHandling(ex -> ex.authenticationEntryPoint((request,response,exception)->response.sendRedirect("/login")));
        return http.build();
    }

    @Bean @Order(2)
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http,AuthServerProperties properties) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/login","/error","/css/**","/favicon.ico","/actuator/health","/.well-known/**").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
            .csrf(csrf -> csrf.ignoringRequestMatchers(request -> AdminBootstrapTokenFilter.isValid(request,properties)))
            .formLogin(form -> form.loginPage("/login").permitAll())
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'; object-src 'none'; img-src 'self'; style-src 'self'; script-src 'self'; connect-src 'self'"))
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)));
        return http.build();
    }

    @Bean AuthorizationServerSettings authorizationServerSettings(AuthServerProperties properties){return AuthorizationServerSettings.builder().issuer(properties.issuer()).build();}
    @Bean CorsConfigurationSource oauthCorsConfigurationSource(@Value("${authserver.demo-spa-origin:http://localhost:4173}") String spaOrigin){
        CorsConfiguration config=new CorsConfiguration();config.setAllowedOrigins(List.of(spaOrigin));config.setAllowedMethods(List.of(HttpMethod.GET.name(),HttpMethod.POST.name(),HttpMethod.OPTIONS.name()));config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION,HttpHeaders.CONTENT_TYPE));config.setExposedHeaders(List.of(HttpHeaders.WWW_AUTHENTICATE));config.setAllowCredentials(false);config.setMaxAge(600L);
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/oauth2/token",config);source.registerCorsConfiguration("/oauth2/jwks",config);source.registerCorsConfiguration("/userinfo",config);return source;
    }
    @Bean AuthenticationEventPublisher authenticationEventPublisher(org.springframework.context.ApplicationEventPublisher events){return new DefaultAuthenticationEventPublisher(events);}
}

