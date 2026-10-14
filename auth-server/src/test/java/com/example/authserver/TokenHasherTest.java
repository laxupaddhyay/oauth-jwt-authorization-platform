package com.example.authserver;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import com.example.authserver.service.authorization.TokenHasher;

class TokenHasherTest {
    @Test void producesLowercaseSha256Hex(){assertThat(new TokenHasher().sha256Hex("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");}
}
