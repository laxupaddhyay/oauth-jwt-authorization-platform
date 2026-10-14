package com.example.authserver.validation;
import com.example.authserver.web.admin.dto.ClientCreateRequest; import org.springframework.core.annotation.Order; import org.springframework.stereotype.Component;
@Component @Order(5) public class SecretRule implements ClientRegistrationRule {
 @Override public void validate(ClientCreateRequest r){if(r.clientType().equals("PUBLIC")&&!r.authMethods().equals(java.util.Set.of("none")))throw new IllegalArgumentException("Public clients must declare only none authentication"); if(r.clientType().equals("PUBLIC")&&r.requirePkce()!=null&&!r.requirePkce())throw new IllegalArgumentException("PKCE is mandatory for public clients");}
}
