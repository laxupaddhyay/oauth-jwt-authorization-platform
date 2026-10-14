package com.example.authserver;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.example.authserver.validation.GrantTypeRule;
import com.example.authserver.validation.ClientTypeRule;
import com.example.authserver.web.admin.dto.ClientCreateRequest;
import java.util.List; import java.util.Set;
import org.junit.jupiter.api.Test;

class ClientRegistrationRulesTest {
    private ClientCreateRequest request(String type,Set<String> grants,List<String> redirects){return new ClientCreateRequest("Demo",type,grants,type.equals("PUBLIC")?Set.of("none"):Set.of("client_secret_basic"),Set.of("openid"),redirects,List.of(),true,true);}
    @Test void rejectsPasswordImplicitAndConfidentialOnlyGrantForPublic(){var grant=new GrantTypeRule();assertThatThrownBy(()->grant.validate(request("CONFIDENTIAL",Set.of("password"),List.of()))).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->grant.validate(request("CONFIDENTIAL",Set.of("implicit"),List.of()))).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->grant.validate(request("PUBLIC",Set.of("client_credentials"),List.of()))).isInstanceOf(IllegalArgumentException.class);}
    @Test void requiresRedirectForCodeAndAcceptsServiceClient(){var grant=new GrantTypeRule();assertThatThrownBy(()->grant.validate(request("CONFIDENTIAL",Set.of("authorization_code"),List.of()))).isInstanceOf(IllegalArgumentException.class);assertDoesNotThrow(()->grant.validate(request("CONFIDENTIAL",Set.of("client_credentials"),List.of())));}
    @Test void publicClientsCannotDisablePkceOrUseSecretAuthentication(){var rule=new ClientTypeRule();var bad=new ClientCreateRequest("Demo","PUBLIC",Set.of("authorization_code"),Set.of("client_secret_basic"),Set.of("openid"),List.of("https://app.example/cb"),List.of(),false,true);assertThatThrownBy(()->rule.validate(bad)).isInstanceOf(IllegalArgumentException.class);}
}
