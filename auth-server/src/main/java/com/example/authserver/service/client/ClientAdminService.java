package com.example.authserver.service.client;

import com.example.authserver.config.AuthServerProperties;
import com.example.authserver.domain.client.OAuthClient;
import com.example.authserver.repository.OAuthClientRepository;
import com.example.authserver.validation.ClientRegistrationValidator;
import com.example.authserver.web.admin.dto.ClientCreateRequest;
import com.example.authserver.web.admin.dto.ClientResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientAdminService {
    private final OAuthClientRepository entities; private final RegisteredClientRepository clients; private final ClientRegistrationValidator validator; private final PasswordEncoder passwords; private final CachingRegisteredClientRepository cache; private final AuthServerProperties properties; private final SecureRandom random=new SecureRandom();
    public ClientAdminService(OAuthClientRepository entities,RegisteredClientRepository clients,ClientRegistrationValidator validator,PasswordEncoder passwords,CachingRegisteredClientRepository cache,AuthServerProperties properties){this.entities=entities;this.clients=clients;this.validator=validator;this.passwords=passwords;this.cache=cache;this.properties=properties;}

    @Transactional
    public ClientResponse create(ClientCreateRequest request){
        validator.validate(request); String clientId=randomToken(24); String secret=request.clientType().equals("CONFIDENTIAL")?randomToken(32):null;
        var builder=RegisteredClient.withId(UUID.randomUUID().toString()).clientId(clientId).clientName(request.clientName().trim())
            .clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(request.consentRequired()).build())
            .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(properties.tokens().accessTokenTtl()).refreshTokenTimeToLive(properties.tokens().refreshTokenTtl()).authorizationCodeTimeToLive(properties.tokens().authorizationCodeTtl()).reuseRefreshTokens(false).build());
        builder.clientSecret(secret==null?null:passwords.encode(secret));
        request.authMethods().forEach(method->builder.clientAuthenticationMethod(new ClientAuthenticationMethod(method)));
        request.grantTypes().forEach(grant->builder.authorizationGrantType(toGrant(grant)));
        if(request.redirectUris()!=null)request.redirectUris().forEach(builder::redirectUri);
        if(request.postLogoutRedirectUris()!=null)request.postLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
        request.scopes().forEach(builder::scope);
        clients.save(builder.build());
        OAuthClient entity=entities.findByClientId(clientId).orElseThrow();
        cache.evict(clientId);
        return response(entity,secret);
    }

    @Transactional(readOnly=true)
    public Page<ClientResponse> list(String filter,Pageable pageable){
        Specification<OAuthClient> spec=(root,query,cb)->filter==null||filter.isBlank()?cb.conjunction():cb.like(cb.lower(root.get("clientName")),"%"+filter.toLowerCase()+"%");
        return entities.findAll(spec,pageable).map(c->response(c,null));
    }
    @Transactional(readOnly=true)
    public ClientResponse get(String clientId){return entities.findByClientId(clientId).map(c->response(c,null)).orElseThrow(()->new ClientNotFoundException(clientId));}
    @Transactional
    public void delete(String clientId){var client=entities.findByClientId(clientId).orElseThrow(()->new ClientNotFoundException(clientId));entities.delete(client);cache.evict(clientId);}
    @Transactional
    public ClientResponse rotateSecret(String clientId){
        var registered=clients.findByClientId(clientId);if(registered==null||registered.getClientSecret()==null)throw new IllegalArgumentException("A confidential client is required to rotate a secret");
        String secret=randomToken(32);var replacement=RegisteredClient.from(registered).clientSecret(passwords.encode(secret)).build();clients.save(replacement);cache.evict(clientId);return get(clientId,secret);
    }
    private String clientIdWithSecret(String id){return id;}
    private ClientResponse get(String id,String secret){var entity=entities.findByClientId(id).orElseThrow(()->new ClientNotFoundException(id));return response(entity,secret);}
    private ClientResponse response(OAuthClient entity,String secret){var redirectUris=entity.getRedirectUris().stream().filter(r->r.getUriType().equals("REDIRECT")).map(r->r.getUri()).collect(java.util.stream.Collectors.toSet());return new ClientResponse(entity.getClientId(),entity.getClientName(),entity.getClientType(),Set.copyOf(entity.getGrantTypes()),Set.copyOf(entity.getScopes()),redirectUris,entity.isEnabled(),secret);}
    private String randomToken(int bytes){byte[] value=new byte[bytes];random.nextBytes(value);return Base64.getUrlEncoder().withoutPadding().encodeToString(value);}
    private AuthorizationGrantType toGrant(String value){return switch(value){case "authorization_code"->AuthorizationGrantType.AUTHORIZATION_CODE;case "refresh_token"->AuthorizationGrantType.REFRESH_TOKEN;case "client_credentials"->AuthorizationGrantType.CLIENT_CREDENTIALS;default->throw new IllegalArgumentException("Unsupported grant type");};}
    public static class ClientNotFoundException extends RuntimeException{public ClientNotFoundException(String id){super("Client not found: "+id);}}
}



