package com.example.authserver.service.key;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import java.util.List;

public interface KeyProvider { JWK activeSigningKey(); JWKSet publishedKeySet(); List<JWK> publishedKeys(); }
