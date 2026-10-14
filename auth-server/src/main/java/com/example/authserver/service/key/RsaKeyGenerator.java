package com.example.authserver.service.key;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import org.springframework.stereotype.Component;

@Component
public class RsaKeyGenerator implements KeyGenerator {
    @Override public KeyPair generate(){try{KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);return generator.generateKeyPair();}catch(GeneralSecurityException e){throw new IllegalStateException("Unable to generate RSA signing key",e);}}
}
