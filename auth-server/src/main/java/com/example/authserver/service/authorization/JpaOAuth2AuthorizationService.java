package com.example.authserver.service.authorization;

import com.example.authserver.domain.authorization.OAuthAuthorization;
import com.example.authserver.repository.OAuthAuthorizationRepository;
import com.example.authserver.repository.OAuthTokenRepository;
import java.util.UUID;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
public class JpaOAuth2AuthorizationService implements OAuth2AuthorizationService {
    private final OAuthAuthorizationRepository authorizations; private final OAuthTokenRepository tokens; private final AuthorizationMapper mapper; private final RefreshTokenFamilyService families; private final TokenHasher hasher;
    public JpaOAuth2AuthorizationService(OAuthAuthorizationRepository authorizations,OAuthTokenRepository tokens,AuthorizationMapper mapper,RefreshTokenFamilyService families,TokenHasher hasher){this.authorizations=authorizations;this.tokens=tokens;this.mapper=mapper;this.families=families;this.hasher=hasher;}
    @Override @Transactional public void save(org.springframework.security.oauth2.server.authorization.OAuth2Authorization authorization){
        Assert.notNull(authorization,"authorization cannot be null");UUID id=UUID.fromString(authorization.getId());OAuthAuthorization existing=authorizations.findById(id).orElse(null);OAuthAuthorization entity=mapper.toEntity(authorization,existing);authorizations.save(entity);families.linkRotatedRefreshToken(entity);
    }
    @Override @Transactional public void remove(org.springframework.security.oauth2.server.authorization.OAuth2Authorization authorization){Assert.notNull(authorization,"authorization cannot be null");authorizations.deleteById(UUID.fromString(authorization.getId()));}
    @Override @Transactional(readOnly=true) public org.springframework.security.oauth2.server.authorization.OAuth2Authorization findById(String id){try{return authorizations.findById(UUID.fromString(id)).map(mapper::toObject).orElse(null);}catch(IllegalArgumentException invalid){return null;}}
    @Override @Transactional(readOnly=true) public org.springframework.security.oauth2.server.authorization.OAuth2Authorization findByToken(String value,OAuth2TokenType tokenType){
        if(value==null)return null;var hash=hasher.sha256Hex(value);var row=tokenType==null?tokens.findByValueHash(hash):tokens.findByTokenTypeAndValueHash(persistedType(tokenType),hash);
        return row.map(token->authorizations.findById(token.getAuthorization().getId()).map(mapper::toObject).orElse(null)).orElse(null);
    }
    private String persistedType(OAuth2TokenType type){return switch(type.getValue()){case "code"->"AUTHORIZATION_CODE";case "access_token"->"ACCESS_TOKEN";case "refresh_token"->"REFRESH_TOKEN";case "id_token"->"ID_TOKEN";default->type.getValue().toUpperCase(java.util.Locale.ROOT);};}
}

