package com.example.authserver.service.key;

import com.example.authserver.config.AuthServerProperties;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class KeyEncryptor {
    private static final int IV_LENGTH=12; private static final int TAG_BITS=128;
    private final SecretKeySpec key; private final SecureRandom random=new SecureRandom();
    public KeyEncryptor(AuthServerProperties properties){byte[] raw;try{raw=Base64.getDecoder().decode(properties.keys().encryptionSecret());}catch(IllegalArgumentException e){throw new IllegalStateException("KEY_ENCRYPTION_SECRET must be base64 encoded",e);}if(raw.length!=32)throw new IllegalStateException("KEY_ENCRYPTION_SECRET must decode to exactly 32 bytes");key=new SecretKeySpec(raw,"AES");Arrays.fill(raw,(byte)0);}
    public byte[] encrypt(byte[] plaintext){byte[] iv=new byte[IV_LENGTH];random.nextBytes(iv);try{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(TAG_BITS,iv));byte[] ciphertext=cipher.doFinal(plaintext);return ByteBuffer.allocate(iv.length+ciphertext.length).put(iv).put(ciphertext).array();}catch(GeneralSecurityException e){throw new IllegalStateException("Unable to encrypt signing key",e);}}
    public byte[] decrypt(byte[] encrypted){if(encrypted==null||encrypted.length<=IV_LENGTH)throw new IllegalArgumentException("Encrypted signing key is malformed");byte[] iv=Arrays.copyOfRange(encrypted,0,IV_LENGTH);byte[] ciphertext=Arrays.copyOfRange(encrypted,IV_LENGTH,encrypted.length);try{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(TAG_BITS,iv));return cipher.doFinal(ciphertext);}catch(GeneralSecurityException e){throw new IllegalStateException("Unable to decrypt signing key; check KEY_ENCRYPTION_SECRET and key integrity",e);}}
}
