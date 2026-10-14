package com.example.authserver.domain.key;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="signing_key",indexes=@Index(name="idx_signing_key_status",columnList="status"))
public class SigningKey {
    @Id @Column(length=64) private String kid;
    @Column(nullable=false,length=16) private String algorithm="RS256";
    @Column(name="key_type",nullable=false,length=8) private String keyType="RSA";
    @Column(name="public_key_pem",nullable=false,columnDefinition="text") private String publicKeyPem;
    @Column(name="private_key_encrypted",nullable=false,columnDefinition="bytea") private byte[] privateKeyEncrypted;
    @Column(nullable=false,length=16) private String status="PENDING";
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    @Column(name="activated_at") private Instant activatedAt;
    @Column(name="retire_at") private Instant retireAt;
    @Column(name="expires_at") private Instant expiresAt;
    protected SigningKey(){}
    public SigningKey(String kid,String algorithm,String keyType,String publicKeyPem,byte[] encrypted){this.kid=kid;this.algorithm=algorithm;this.keyType=keyType;this.publicKeyPem=publicKeyPem;this.privateKeyEncrypted=encrypted;}
    public String getKid(){return kid;} public String getAlgorithm(){return algorithm;} public String getKeyType(){return keyType;} public String getPublicKeyPem(){return publicKeyPem;} public byte[] getPrivateKeyEncrypted(){return privateKeyEncrypted;} public String getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public Instant getActivatedAt(){return activatedAt;} public Instant getRetireAt(){return retireAt;} public Instant getExpiresAt(){return expiresAt;}
    public void setStatus(String v){status=v;} public void setActivatedAt(Instant v){activatedAt=v;} public void setRetireAt(Instant v){retireAt=v;} public void setExpiresAt(Instant v){expiresAt=v;}
}
