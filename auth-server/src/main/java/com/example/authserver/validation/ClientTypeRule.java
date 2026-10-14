package com.example.authserver.validation;
import com.example.authserver.web.admin.dto.ClientCreateRequest; import org.springframework.core.annotation.Order; import org.springframework.stereotype.Component;
@Component @Order(3) public class ClientTypeRule implements ClientRegistrationRule {
 @Override public void validate(ClientCreateRequest r){
  if(!r.clientType().equals("PUBLIC")&&!r.clientType().equals("CONFIDENTIAL")) throw new IllegalArgumentException("clientType must be PUBLIC or CONFIDENTIAL");
  if(r.requirePkce()!=null&&!r.requirePkce()) throw new IllegalArgumentException("PKCE is mandatory for every client");
  if(r.clientType().equals("PUBLIC")&&r.authMethods().stream().anyMatch(m->!m.equals("none"))) throw new IllegalArgumentException("Public clients must use the none authentication method");
  if(r.clientType().equals("CONFIDENTIAL")&&r.authMethods().stream().anyMatch(m->!m.equals("client_secret_basic")&&!m.equals("client_secret_post"))) throw new IllegalArgumentException("Confidential clients must use a supported client secret authentication method");
  if(r.clientType().equals("CONFIDENTIAL")&&r.authMethods().isEmpty()) throw new IllegalArgumentException("Confidential clients require a client authentication method");
 }
}

