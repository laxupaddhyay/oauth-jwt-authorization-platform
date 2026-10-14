package com.example.authserver.service.authorization;

import com.example.authserver.domain.authorization.OAuthAuthorization;
import com.example.authserver.domain.authorization.OAuthToken;
import com.example.authserver.domain.client.OAuthClient;
import com.example.authserver.domain.user.AppUser;
import com.example.authserver.repository.AppUserRepository;
import com.example.authserver.repository.OAuthClientRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationMapper {
    private static final TypeReference<Map<String,Object>> MAP_TYPE=new TypeReference<>(){};
    private final ObjectMapper json;
    private final RegisteredClientRepository registeredClients;
    private final OAuthClientRepository clientEntities;
    private final AppUserRepository users;
    private final TokenHasher hasher;

    public AuthorizationMapper(RegisteredClientRepository registeredClients,OAuthClientRepository clientEntities,AppUserRepository users,TokenHasher hasher){
        this.registeredClients=registeredClients;this.clientEntities=clientEntities;this.users=users;this.hasher=hasher;
        this.json=new ObjectMapper();ClassLoader loader=getClass().getClassLoader();List<Module> modules=SecurityJackson2Modules.getModules(loader);this.json.registerModules(modules);this.json.registerModule(new OAuth2AuthorizationServerJackson2Module());
    }
    public OAuthAuthorization toEntity(org.springframework.security.oauth2.server.authorization.OAuth2Authorization source,OAuthAuthorization existing){
        RegisteredClient registered=registeredClients.findById(source.getRegisteredClientId());
        if(registered==null)throw new DataRetrievalFailureException("Registered client is missing");
        OAuthClient client=clientEntities.findByClientId(registered.getClientId()).orElseThrow(()->new DataRetrievalFailureException("Client entity is missing"));
        OAuthAuthorization target=existing==null?new OAuthAuthorization():existing;
        target.setId(java.util.UUID.fromString(source.getId()));target.setClient(client);target.setPrincipalName(source.getPrincipalName());target.setGrantType(source.getAuthorizationGrantType().getValue());target.setAuthorizedScopes(source.getAuthorizedScopes().toArray(String[]::new));
        target.setAttributes(Map.of("serialized_attributes",write(source.getAttributes())));
        target.setStateHash(stateHash(source.getAttributes()));
        target.setUser(users.findByUsername(source.getPrincipalName()).orElse(null));
        if(existing==null)target.setFamilyId(java.util.UUID.randomUUID());
        mergeTokens(target,source);
        return target;
    }
    public org.springframework.security.oauth2.server.authorization.OAuth2Authorization toObject(OAuthAuthorization source){
        RegisteredClient client=registeredClients.findById(source.getClient().getId().toString());
        if(client==null)throw new DataRetrievalFailureException("Registered client is missing");
        var builder=org.springframework.security.oauth2.server.authorization.OAuth2Authorization.withRegisteredClient(client)
            .id(source.getId().toString()).principalName(source.getPrincipalName())
            .authorizationGrantType(new AuthorizationGrantType(source.getGrantType()))
            .authorizedScopes(new HashSet<>(List.of(source.getAuthorizedScopes())));
        readMap(serializedAttributes(source.getAttributes())).forEach(builder::attribute);
        for(OAuthToken row:source.getTokens()) addToken(builder,row);
        return builder.build();
    }
    private void mergeTokens(OAuthAuthorization target,org.springframework.security.oauth2.server.authorization.OAuth2Authorization source){
        Map<String,OAuthToken> old=new HashMap<>();for(OAuthToken token:target.getTokens())old.put(token.getTokenType()+":"+token.getValueHash(),token);
        List<OAuthToken> present=new ArrayList<>();
        addRow(source.getToken(OAuth2AuthorizationCode.class),"AUTHORIZATION_CODE",target,old,present);
        addRow(source.getAccessToken(),"ACCESS_TOKEN",target,old,present);
        addRow(source.getRefreshToken(),"REFRESH_TOKEN",target,old,present);
        addRow(source.getToken(OidcIdToken.class),"ID_TOKEN",target,old,present);
        for(OAuthToken token:old.values())if(!present.contains(token))present.add(token);
        target.setTokens(present);
    }
    private <T extends org.springframework.security.oauth2.core.OAuth2Token> void addRow(org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Token<T> wrapper,String type,OAuthAuthorization auth,Map<String,OAuthToken> old,List<OAuthToken> present){
        if(wrapper==null)return;T token=wrapper.getToken();String hash=hasher.sha256Hex(token.getTokenValue());OAuthToken row=old.remove(type+":"+hash);if(row==null)row=new OAuthToken();row.setAuthorization(auth);row.setTokenType(type);row.setValue(token.getTokenValue());row.setValueHash(hash);row.setIssuedAt(token.getIssuedAt());row.setExpiresAt(token.getExpiresAt());row.setScopes(token instanceof OAuth2AccessToken access?access.getScopes().toArray(String[]::new):new String[0]);row.setMetadata(readMap(write(wrapper.getMetadata())));Object jti=wrapper.getMetadata().get("claims") instanceof Map<?,?> claims?claims.get("jti"):null;row.setJti(jti==null?null:jti.toString());row.setInvalidated(Boolean.TRUE.equals(wrapper.getMetadata().get("invalidated")));row.setInvalidatedAt(row.isInvalidated()?Instant.now():null);present.add(row);
    }
    private void addToken(org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Builder builder,OAuthToken row){
        Map<String,Object> metadata=row.getMetadata()==null?Map.of():row.getMetadata();String type=row.getTokenType();org.springframework.security.oauth2.core.OAuth2Token token=switch(type){
            case "AUTHORIZATION_CODE"->new OAuth2AuthorizationCode(row.getValue(),row.getIssuedAt(),row.getExpiresAt());
            case "ACCESS_TOKEN"->new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,row.getValue(),row.getIssuedAt(),row.getExpiresAt(),Set.of(row.getScopes()));
            case "REFRESH_TOKEN"->new OAuth2RefreshToken(row.getValue(),row.getIssuedAt(),row.getExpiresAt());
            case "ID_TOKEN"->idToken(row,metadata);
            default->throw new DataRetrievalFailureException("Unknown persisted token type");
        };
        builder.token(token,map->map.putAll(metadata));
    }
    private OidcIdToken idToken(OAuthToken row,Map<String,Object> metadata){Object value=metadata.get("claims");Map<String,Object> claims=value instanceof Map<?,?> map?castMap(map):Map.of();return new OidcIdToken(row.getValue(),row.getIssuedAt(),row.getExpiresAt(),claims);}
    @SuppressWarnings("unchecked") private Map<String,Object> castMap(Map<?,?> map){return (Map<String,Object>)map;}
    private String serializedAttributes(Map<String,Object> attributes){Object payload=attributes.get("serialized_attributes");if(!(payload instanceof String value))throw new IllegalStateException("Persisted OAuth attributes are malformed");return value;}
    private String stateHash(Map<String,Object> attributes){Object request=attributes.get(org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest.class.getName());if(request instanceof org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest authRequest)return authRequest.getState()==null?null:hasher.sha256Hex(authRequest.getState());Object value=attributes.get(org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.STATE);return value instanceof String state?hasher.sha256Hex(state):null;}
    private String write(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException ex){throw new IllegalStateException("OAuth state could not be safely serialized",ex);}}
    private Map<String,Object> readMap(String value){try{return json.readValue(value,MAP_TYPE);}catch(JsonProcessingException ex){throw new IllegalStateException("OAuth state could not be safely deserialized",ex);}}
}



