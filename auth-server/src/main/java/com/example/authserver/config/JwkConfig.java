package com.example.authserver.config;

import com.example.authserver.service.key.KeyProvider;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

@Configuration
public class JwkConfig {
    /** Public-only source is shared with discovery/JWKS and signature validation. */
    @Bean @Primary JWKSource<SecurityContext> jwkSource(KeyProvider provider){return (selector,context)->selector.select(provider.publishedKeySet());}
    /** The encoder alone sees private key material, limited to the currently active signer. */
    @Bean("signingJwkSource") JWKSource<SecurityContext> signingJwkSource(KeyProvider provider){return (selector,context)->selector.select(new JWKSet(provider.activeSigningKey()));}
    @Bean JwtEncoder jwtEncoder(@Qualifier("signingJwkSource") JWKSource<SecurityContext> signingSource){return new NimbusJwtEncoder(signingSource);}
    @Bean JwtDecoder jwtDecoder(@Qualifier("jwkSource") JWKSource<SecurityContext> publicSource){return OAuth2AuthorizationServerConfiguration.jwtDecoder(publicSource);}
    @Bean OAuth2TokenGenerator<?> tokenGenerator(JwtEncoder encoder){var jwt=new JwtGenerator(encoder);return new DelegatingOAuth2TokenGenerator(jwt,new OAuth2AccessTokenGenerator(),new OAuth2RefreshTokenGenerator());}
}
