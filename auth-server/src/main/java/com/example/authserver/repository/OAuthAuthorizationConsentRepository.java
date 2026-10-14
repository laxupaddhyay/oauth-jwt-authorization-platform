package com.example.authserver.repository;
import com.example.authserver.domain.authorization.OAuthAuthorizationConsent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OAuthAuthorizationConsentRepository extends JpaRepository<OAuthAuthorizationConsent, OAuthAuthorizationConsent.Key> {
    void deleteByClientPkAndPrincipalName(UUID clientPk,String principalName);
}
