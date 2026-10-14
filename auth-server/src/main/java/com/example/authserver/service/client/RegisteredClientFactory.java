package com.example.authserver.service.client;

import com.example.authserver.domain.client.OAuthClient;
import com.example.authserver.domain.client.OAuthClientRedirectUri;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.stereotype.Component;

@Component
public class RegisteredClientFactory {
    public RegisteredClient fromEntity(OAuthClient entity) {
        var builder = RegisteredClient.withId(entity.getId().toString())
            .clientId(entity.getClientId()).clientIdIssuedAt(entity.getClientIdIssuedAt())
            .clientSecret(entity.getClientSecretHash()).clientSecretExpiresAt(entity.getClientSecretExpiresAt())
            .clientName(entity.getClientName())
            .clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(entity.isRequireConsent()).build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(java.time.Duration.ofSeconds(entity.getAccessTokenTtlSeconds()))
                .refreshTokenTimeToLive(java.time.Duration.ofSeconds(entity.getRefreshTokenTtlSeconds()))
                .authorizationCodeTimeToLive(java.time.Duration.ofSeconds(entity.getAuthorizationCodeTtlSeconds()))
                .reuseRefreshTokens(entity.isReuseRefreshTokens())
                .accessTokenFormat("REFERENCE".equals(entity.getAccessTokenFormat()) ? OAuth2TokenFormat.REFERENCE : OAuth2TokenFormat.SELF_CONTAINED)
                .build());
        entity.getAuthMethods().forEach(method -> builder.clientAuthenticationMethod(new ClientAuthenticationMethod(method)));
        entity.getGrantTypes().forEach(grant -> builder.authorizationGrantType(toGrantType(grant)));
        for (OAuthClientRedirectUri item : entity.getRedirectUris()) {
            if ("REDIRECT".equals(item.getUriType())) builder.redirectUri(item.getUri());
            if ("POST_LOGOUT_REDIRECT".equals(item.getUriType())) builder.postLogoutRedirectUri(item.getUri());
        }
        entity.getScopes().forEach(builder::scope);
        return builder.build();
    }

    public OAuthClient toEntity(RegisteredClient client) {
        var entity = new OAuthClient();
        entity.setId(java.util.UUID.fromString(client.getId()));
        entity.setClientId(client.getClientId());
        entity.setClientSecretHash(client.getClientSecret());
        entity.setClientIdIssuedAt(client.getClientIdIssuedAt());
        entity.setClientSecretExpiresAt(client.getClientSecretExpiresAt());
        entity.setClientName(client.getClientName());
        entity.setClientType(client.getClientSecret() == null ? "PUBLIC" : "CONFIDENTIAL");
        entity.setRequirePkce(client.getClientSettings().isRequireProofKey());
        entity.setRequireConsent(client.getClientSettings().isRequireAuthorizationConsent());
        entity.setAccessTokenFormat(client.getTokenSettings().getAccessTokenFormat().getValue());
        entity.setAccessTokenTtlSeconds((int) client.getTokenSettings().getAccessTokenTimeToLive().toSeconds());
        entity.setRefreshTokenTtlSeconds((int) client.getTokenSettings().getRefreshTokenTimeToLive().toSeconds());
        entity.setAuthorizationCodeTtlSeconds((int) client.getTokenSettings().getAuthorizationCodeTimeToLive().toSeconds());
        entity.setReuseRefreshTokens(client.getTokenSettings().isReuseRefreshTokens());
        Set<String> methods = new HashSet<>(); client.getClientAuthenticationMethods().forEach(m -> methods.add(m.getValue())); entity.setAuthMethods(methods);
        Set<String> grants = new HashSet<>(); client.getAuthorizationGrantTypes().forEach(g -> grants.add(g.getValue())); entity.setGrantTypes(grants);
        entity.setScopes(new HashSet<>(client.getScopes()));
        Set<OAuthClientRedirectUri> uris = new HashSet<>();
        client.getRedirectUris().forEach(uri -> uris.add(new OAuthClientRedirectUri(uri,"REDIRECT")));
        client.getPostLogoutRedirectUris().forEach(uri -> uris.add(new OAuthClientRedirectUri(uri,"POST_LOGOUT_REDIRECT")));
        entity.setRedirectUris(uris);
        return entity;
    }

    private AuthorizationGrantType toGrantType(String value) {
        return switch (value) {
            case "authorization_code" -> AuthorizationGrantType.AUTHORIZATION_CODE;
            case "refresh_token" -> AuthorizationGrantType.REFRESH_TOKEN;
            case "client_credentials" -> AuthorizationGrantType.CLIENT_CREDENTIALS;
            default -> throw new IllegalArgumentException("Unsupported grant type");
        };
    }
}

