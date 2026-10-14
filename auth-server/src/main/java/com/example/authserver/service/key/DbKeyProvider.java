package com.example.authserver.service.key;

import com.example.authserver.domain.key.SigningKey;
import com.example.authserver.repository.SigningKeyRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DbKeyProvider implements KeyProvider,ApplicationRunner {
    private static final long KEY_LOCK=821067351204L;
    private final SigningKeyRepository keys; private final KeyGenerator generator; private final KeyEncryptor encryptor; private final JdbcTemplate jdbc;
    public DbKeyProvider(SigningKeyRepository keys,KeyGenerator generator,KeyEncryptor encryptor,JdbcTemplate jdbc){this.keys=keys;this.generator=generator;this.encryptor=encryptor;this.jdbc=jdbc;}
    @Override @Transactional
    public void run(ApplicationArguments args){jdbc.execute("select pg_advisory_xact_lock("+KEY_LOCK+")");if(keys.findFirstByStatusOrderByCreatedAtDesc("ACTIVE").isEmpty())activateNewKey();}
    @Override @Transactional(readOnly=true) public JWK activeSigningKey(){SigningKey key=keys.findFirstByStatusOrderByCreatedAtDesc("ACTIVE").orElseThrow(()->new IllegalStateException("No active signing key is available"));return toJwk(key,true);}
    @Override @Transactional(readOnly=true) public List<JWK> publishedKeys(){return keys.findByStatusIn(List.of("ACTIVE","RETIRING")).stream().filter(k->k.getExpiresAt()==null||k.getExpiresAt().isAfter(Instant.now())).map(k->toJwk(k,false)).toList();}
    @Override public JWKSet publishedKeySet(){return new JWKSet(publishedKeys());}
    @Transactional protected void activateNewKey(){KeyPair pair=generator.generate();String kid=UUID.randomUUID().toString().replace("-","");String publicPem=pem("PUBLIC KEY",pair.getPublic().getEncoded());SigningKey key=new SigningKey(kid,"RS256","RSA",publicPem,encryptor.encrypt(pair.getPrivate().getEncoded()));key.setStatus("ACTIVE");key.setActivatedAt(Instant.now());keys.saveAndFlush(key);}
    private JWK toJwk(SigningKey entity,boolean includePrivate){try{byte[] encoded=Base64.getDecoder().decode(entity.getPublicKeyPem().replace("-----BEGIN PUBLIC KEY-----","").replace("-----END PUBLIC KEY-----","").replaceAll("\\s",""));var factory=KeyFactory.getInstance("RSA");var publicKey=(RSAPublicKey)factory.generatePublic(new X509EncodedKeySpec(encoded));var builder=new RSAKey.Builder(publicKey).keyID(entity.getKid()).keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.RS256);if(includePrivate){PrivateKey privateKey=factory.generatePrivate(new PKCS8EncodedKeySpec(encryptor.decrypt(entity.getPrivateKeyEncrypted())));builder.privateKey(privateKey);}return builder.build();}catch(Exception e){throw new IllegalStateException("Unable to load signing key",e);}}
    private String pem(String label,byte[] value){return "-----BEGIN "+label+"-----\n"+Base64.getMimeEncoder(64,new byte[]{'\n'}).encodeToString(value)+"\n-----END "+label+"-----";}
}


