package com.example.authserver.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record ClientCreateRequest(
    @NotBlank @Size(max=200) String clientName,
    @NotBlank String clientType,
    @NotEmpty Set<String> grantTypes,
    @NotEmpty Set<String> authMethods,
    @NotEmpty Set<String> scopes,
    List<@NotBlank @Size(max=2048) String> redirectUris,
    List<@NotBlank @Size(max=2048) String> postLogoutRedirectUris,
    Boolean requirePkce,
    Boolean requireConsent
) {
    public List<String> allRedirectUris(){var values=new ArrayList<>(redirectUris==null?List.of():redirectUris);if(postLogoutRedirectUris!=null)values.addAll(postLogoutRedirectUris);return values;}
    public boolean consentRequired(){return requireConsent==null||requireConsent;}
}

