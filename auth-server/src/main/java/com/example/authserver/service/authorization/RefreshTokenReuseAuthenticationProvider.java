package com.example.authserver.service.authorization;

import com.example.authserver.service.key.KeyProvider;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

public class RefreshTokenReuseAuthenticationProvider implements AuthenticationProvider {
    private final RefreshTokenFamilyService families; private final OAuth2RefreshTokenAuthenticationProvider delegate;
    public RefreshTokenReuseAuthenticationProvider(RefreshTokenFamilyService families,OAuth2AuthorizationService authorizations,OAuth2TokenGenerator<?> tokenGenerator){this.families=families;this.delegate=new OAuth2RefreshTokenAuthenticationProvider(authorizations,tokenGenerator);}
    @Override public Authentication authenticate(Authentication authentication){
        var refresh=(OAuth2RefreshTokenAuthenticationToken)authentication;
        if(families.revokeFamilyIfReused(refresh.getRefreshToken()))throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        return delegate.authenticate(authentication);
    }
    @Override public boolean supports(Class<?> authentication){return OAuth2RefreshTokenAuthenticationToken.class.isAssignableFrom(authentication);}
}
