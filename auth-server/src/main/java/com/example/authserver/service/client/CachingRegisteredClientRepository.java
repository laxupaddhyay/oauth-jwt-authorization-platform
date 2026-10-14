package com.example.authserver.service.client;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

@Component @Primary
public class CachingRegisteredClientRepository implements RegisteredClientRepository {
    private final JpaRegisteredClientRepository delegate;
    private final Cache<String,RegisteredClient> byClientId=Caffeine.newBuilder().maximumSize(5_000).expireAfterWrite(Duration.ofMinutes(10)).build();
    private final Cache<String,RegisteredClient> byId=Caffeine.newBuilder().maximumSize(5_000).expireAfterWrite(Duration.ofMinutes(10)).build();
    public CachingRegisteredClientRepository(JpaRegisteredClientRepository delegate){this.delegate=delegate;}
    @Override public void save(RegisteredClient client){delegate.save(client);evict(client.getClientId());cache(client);}
    @Override public RegisteredClient findById(String id){var cached=byId.getIfPresent(id);if(cached!=null)return cached;var found=delegate.findById(id);if(found!=null)cache(found);return found;}
    @Override public RegisteredClient findByClientId(String id){var cached=byClientId.getIfPresent(id);if(cached!=null)return cached;var found=delegate.findByClientId(id);if(found!=null)cache(found);return found;}
    public void evict(String clientId){var previous=byClientId.getIfPresent(clientId);byClientId.invalidate(clientId);if(previous!=null)byId.invalidate(previous.getId());}
    private void cache(RegisteredClient client){byClientId.put(client.getClientId(),client);byId.put(client.getId(),client);}
}
