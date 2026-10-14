package com.example.authserver.validation;
import com.example.authserver.web.admin.dto.ClientCreateRequest; import org.springframework.core.annotation.Order; import org.springframework.stereotype.Component;
@Component @Order(2) public class GrantTypeRule implements ClientRegistrationRule {
 @Override public void validate(ClientCreateRequest r){
  if(r.grantTypes().stream().anyMatch(g->g.equals("implicit")||g.equals("password"))) throw new IllegalArgumentException("implicit and password grants are not supported");
  if(r.grantTypes().isEmpty()||!SetHolder.ALLOWED.containsAll(r.grantTypes())) throw new IllegalArgumentException("Only authorization_code, refresh_token, and client_credentials grants are supported");
  if(r.grantTypes().contains("authorization_code")&&(r.redirectUris()==null||r.redirectUris().isEmpty())) throw new IllegalArgumentException("Authorization code clients must register at least one redirect URI");
  if(r.clientType().equals("PUBLIC")&&(!r.grantTypes().contains("authorization_code")||r.grantTypes().stream().anyMatch(g->!SetHolder.PUBLIC.contains(g)))) throw new IllegalArgumentException("Public clients may use authorization_code and refresh_token only");
  if(r.clientType().equals("PUBLIC")&&r.grantTypes().contains("client_credentials")) throw new IllegalArgumentException("Client credentials requires a confidential client");
 }
 private static final class SetHolder { static final java.util.Set<String> ALLOWED=java.util.Set.of("authorization_code","refresh_token","client_credentials"); static final java.util.Set<String> PUBLIC=java.util.Set.of("authorization_code","refresh_token"); }
}

