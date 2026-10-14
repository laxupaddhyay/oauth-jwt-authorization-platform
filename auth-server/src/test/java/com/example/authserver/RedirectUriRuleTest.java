package com.example.authserver;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.example.authserver.validation.RedirectUriRule;
import com.example.authserver.web.admin.dto.ClientCreateRequest;
import java.util.List; import java.util.Set;
import org.junit.jupiter.api.Test;

class RedirectUriRuleTest {
    private final RedirectUriRule rule=new RedirectUriRule();
    private ClientCreateRequest client(String uri){return new ClientCreateRequest("Demo","PUBLIC",Set.of("authorization_code"),Set.of("none"),Set.of("openid"),List.of(uri),List.of(),true,true);}
    @Test void allowsHttpsAndLoopbackHttp(){assertDoesNotThrow(()->rule.validate(client("https://app.example/callback")));assertDoesNotThrow(()->rule.validate(client("http://localhost:4173/")));assertDoesNotThrow(()->rule.validate(client("http://127.0.0.1/callback")));}
    @Test void rejectsWildcardsFragmentsAndNonLoopbackHttp(){assertThatThrownBy(()->rule.validate(client("https://*.example/callback"))).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->rule.validate(client("https://app.example/callback#fragment"))).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->rule.validate(client("http://app.example/callback"))).isInstanceOf(IllegalArgumentException.class);}
}
