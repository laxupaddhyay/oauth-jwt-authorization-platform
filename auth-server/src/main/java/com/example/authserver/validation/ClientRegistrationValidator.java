package com.example.authserver.validation;

import com.example.authserver.web.admin.dto.ClientCreateRequest;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClientRegistrationValidator {
    private final List<ClientRegistrationRule> rules;
    public ClientRegistrationValidator(List<ClientRegistrationRule> rules){this.rules=List.copyOf(rules);}
    public void validate(ClientCreateRequest request){for(var rule:rules)rule.validate(request);}
}
