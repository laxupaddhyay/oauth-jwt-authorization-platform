package com.example.authserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.example.authserver.config.AuthServerProperties;
import com.example.authserver.service.key.KeyEncryptor;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

class KeyEncryptorTest {
    private KeyEncryptor encryptor(){byte[] key=new byte[32];ThreadLocalRandom.current().nextBytes(key);var props=new AuthServerProperties("issuer",new AuthServerProperties.Tokens(Duration.ofMinutes(1),Duration.ofDays(1),Duration.ofMinutes(1),Duration.ofMinutes(1)),new AuthServerProperties.Keys(Duration.ofDays(1),Duration.ofHours(1),Base64.getEncoder().encodeToString(key)),new AuthServerProperties.Admin("admin"),new AuthServerProperties.RateLimit(10,60));return new KeyEncryptor(props);}
    @Test void encryptsAndAuthenticatesPrivateMaterial(){var service=encryptor();byte[] privateKey="private-key-material".getBytes(java.nio.charset.StandardCharsets.UTF_8);byte[] ciphertext=service.encrypt(privateKey);assertThat(ciphertext).isNotEqualTo(privateKey);assertThat(service.decrypt(ciphertext)).isEqualTo(privateKey);ciphertext[ciphertext.length-1]^=1;assertThatThrownBy(()->service.decrypt(ciphertext)).isInstanceOf(IllegalStateException.class);}
}
