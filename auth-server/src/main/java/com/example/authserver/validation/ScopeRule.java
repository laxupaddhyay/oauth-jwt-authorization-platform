package com.example.authserver.validation;
import com.example.authserver.repository.OAuthScopeRepository; import com.example.authserver.web.admin.dto.ClientCreateRequest; import java.util.Set; import java.util.stream.Collectors; import org.springframework.core.annotation.Order; import org.springframework.stereotype.Component;
@Component @Order(4) public class ScopeRule implements ClientRegistrationRule {
 private final OAuthScopeRepository scopes; public ScopeRule(OAuthScopeRepository scopes){this.scopes=scopes;}
 @Override public void validate(ClientCreateRequest request){Set<String> present=scopes.findAllById(request.scopes()).stream().map(s->s.getName()).collect(Collectors.toSet()); if(!present.containsAll(request.scopes())) throw new IllegalArgumentException("One or more requested scopes are not registered");}
}
