package com.example.authserver.service.client;

import com.example.authserver.domain.client.OAuthClient;
import com.example.authserver.repository.OAuthClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Repository
public class JpaRegisteredClientRepository implements RegisteredClientRepository {
    private final OAuthClientRepository clients;
    private final RegisteredClientFactory factory;
    public JpaRegisteredClientRepository(OAuthClientRepository clients, RegisteredClientFactory factory) { this.clients=clients; this.factory=factory; }
    @Override @Transactional
    public void save(RegisteredClient registeredClient) {
        Assert.notNull(registeredClient,"registeredClient must not be null");
        OAuthClient entity = clients.findByClientId(registeredClient.getClientId()).orElseGet(OAuthClient::new);
        OAuthClient mapped = factory.toEntity(registeredClient);
        if (entity.getId() != null) mapped = copyIdentity(mapped, entity);
        clients.save(mapped);
    }
    private OAuthClient copyIdentity(OAuthClient mapped, OAuthClient existing) { mapped.setId(existing.getId()); mapped.setClientIdIssuedAt(existing.getClientIdIssuedAt()); mapped.setCreatedAt(existing.getCreatedAt()); mapped.setCreatedBy(existing.getCreatedBy()); return mapped; }
    @Override @Transactional(readOnly=true)
    public RegisteredClient findById(String id) {
        try { return clients.findById(java.util.UUID.fromString(id)).filter(OAuthClient::isEnabled).map(factory::fromEntity).orElse(null); }
        catch (IllegalArgumentException invalidId) { return null; }
    }
    @Override @Transactional(readOnly=true)
    public RegisteredClient findByClientId(String clientId) {
        if (clientId == null) return null;
        return clients.findByClientId(clientId).filter(OAuthClient::isEnabled).map(factory::fromEntity).orElse(null);
    }
}

