package com.example.authserver.repository;
import com.example.authserver.domain.client.OAuthClient;
import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface OAuthClientRepository extends JpaRepository<OAuthClient, UUID>,JpaSpecificationExecutor<OAuthClient> { Optional<OAuthClient> findByClientId(String clientId); boolean existsByClientId(String clientId); }
