package com.example.authserver.web.admin.dto;

import java.time.Instant;
import java.util.Set;

public record ClientResponse(String clientId,String clientName,String clientType,Set<String> grantTypes,Set<String> scopes,Set<String> redirectUris,boolean enabled,String clientSecret) {
    public static ClientResponse safe(String id,String name,String type,Set<String> grants,Set<String> scopes,Set<String> redirects,boolean enabled){return new ClientResponse(id,name,type,Set.copyOf(grants),Set.copyOf(scopes),Set.copyOf(redirects),enabled,null);}
}
