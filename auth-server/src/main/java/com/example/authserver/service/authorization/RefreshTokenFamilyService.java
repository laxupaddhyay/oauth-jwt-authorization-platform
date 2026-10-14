package com.example.authserver.service.authorization;

import com.example.authserver.domain.audit.AuditEvent;
import com.example.authserver.domain.authorization.OAuthAuthorization;
import com.example.authserver.domain.authorization.OAuthToken;
import com.example.authserver.repository.AuditEventRepository;
import com.example.authserver.repository.OAuthAuthorizationRepository;
import com.example.authserver.repository.OAuthTokenRepository;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenFamilyService {
    private final OAuthTokenRepository tokens; private final OAuthAuthorizationRepository authorizations; private final TokenHasher hasher; private final AuditEventRepository audit;
    public RefreshTokenFamilyService(OAuthTokenRepository tokens,OAuthAuthorizationRepository authorizations,TokenHasher hasher,AuditEventRepository audit){this.tokens=tokens;this.authorizations=authorizations;this.hasher=hasher;this.audit=audit;}
    @Transactional
    public boolean revokeFamilyIfReused(String rawRefreshToken){
        var token=tokens.findByTokenTypeAndValueHash("REFRESH_TOKEN",hasher.sha256Hex(rawRefreshToken));
        if(token.isEmpty()||!token.get().isInvalidated())return false;
        UUID family=token.get().getAuthorization().getFamilyId();
        var locked=authorizations.findLockedByFamilyId(family);
        locked.forEach(a->a.revoke("REFRESH_TOKEN_REUSE"));
        tokens.invalidateFamily(family);
        audit.save(new AuditEvent("REFRESH_REUSE_DETECTED",token.get().getAuthorization().getPrincipalName(),false,JsonNodeFactory.instance.objectNode().put("family_id",family.toString())));
        return true;
    }
    @Transactional
    public void linkRotatedRefreshToken(OAuthAuthorization authorization){
        var familyTokens=tokens.findByFamilyId(authorization.getFamilyId());
        var latest=familyTokens.stream().filter(t->t.getTokenType().equals("REFRESH_TOKEN")&&!t.isInvalidated()).max(Comparator.comparing(OAuthToken::getIssuedAt)).orElse(null);
        if(latest==null)return;
        familyTokens.stream().filter(t->t.getTokenType().equals("REFRESH_TOKEN")&&t.isInvalidated()&&t.getRotatedTo()==null&&t.getIssuedAt()!=null&&t.getIssuedAt().isBefore(latest.getIssuedAt())).forEach(t->t.setRotatedTo(latest));
    }
    @Transactional(readOnly=true) public boolean isRevoked(UUID familyId){return authorizations.findByFamilyId(familyId).stream().anyMatch(OAuthAuthorization::isRevoked);}
}
