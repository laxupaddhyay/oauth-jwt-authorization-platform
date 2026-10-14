package com.example.authserver.validation;

import com.example.authserver.web.admin.dto.ClientCreateRequest;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component @Order(1)
public class RedirectUriRule implements ClientRegistrationRule {
    @Override public void validate(ClientCreateRequest request) {
        for (String value : request.allRedirectUris()) {
            try {
                URI uri = new URI(value);
                if (!uri.isAbsolute() || uri.getFragment()!=null || value.contains("*") || uri.getHost()==null)
                    throw new IllegalArgumentException("Redirect URIs must be absolute, exact, and must not contain fragments or wildcards");
                boolean local = uri.getHost().equalsIgnoreCase("localhost") || uri.getHost().equals("127.0.0.1") || uri.getHost().equals("::1");
                if (!"https".equalsIgnoreCase(uri.getScheme()) && !(local && "http".equalsIgnoreCase(uri.getScheme())))
                    throw new IllegalArgumentException("Redirect URIs must use HTTPS except for loopback development hosts");
                if (uri.getUserInfo()!=null) throw new IllegalArgumentException("Redirect URIs must not contain user information");
            } catch (URISyntaxException e) { throw new IllegalArgumentException("A redirect URI is malformed",e); }
        }
    }
}
