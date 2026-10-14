package com.example.authserver.service.authorization;

import com.example.authserver.domain.authorization.OAuthAuthorizationConsent;
import com.example.authserver.repository.OAuthAuthorizationConsentRepository;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
public class JpaOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {
    private final OAuthAuthorizationConsentRepository consents; private final RegisteredClientRepository registeredClients;
    public JpaOAuth2AuthorizationConsentService(OAuthAuthorizationConsentRepository consents,RegisteredClientRepository registeredClients){this.consents=consents;this.registeredClients=registeredClients;}
    @Override @Transactional public void save(org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent consent){Assert.notNull(consent,"consent cannot be null");var entity=new OAuthAuthorizationConsent();entity.setClientPk(clientPk(consent.getRegisteredClientId()));entity.setPrincipalName(consent.getPrincipalName());entity.setAuthorities(consent.getAuthorities().stream().map(a->a.getAuthority()).sorted().toArray(String[]::new));consents.save(entity);}
    @Override @Transactional public void remove(org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent consent){Assert.notNull(consent,"consent cannot be null");consents.deleteByClientPkAndPrincipalName(clientPk(consent.getRegisteredClientId()),consent.getPrincipalName());}
    @Override @Transactional(readOnly=true) public org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent findById(String registeredClientId,String principalName){Assert.hasText(registeredClientId,"registeredClientId cannot be blank");Assert.hasText(principalName,"principalName cannot be blank");return consents.findById(new OAuthAuthorizationConsent.Key(clientPk(registeredClientId),principalName)).map(entity->{if(registeredClients.findById(registeredClientId)==null)throw new DataRetrievalFailureException("Registered client is missing");var builder=org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent.withId(registeredClientId,principalName);Arrays.stream(entity.getAuthorities()).map(SimpleGrantedAuthority::new).forEach(a->builder.authority(a));return builder.build();}).orElse(null);}
    private UUID clientPk(String id){try{return UUID.fromString(id);}catch(IllegalArgumentException ex){throw new DataRetrievalFailureException("Invalid registered client identifier",ex);}}
}
