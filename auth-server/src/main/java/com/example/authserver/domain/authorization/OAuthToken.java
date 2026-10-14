package com.example.authserver.domain.authorization;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="oauth_token", indexes={@Index(name="idx_token_authorization",columnList="authorization_id"),@Index(name="idx_token_expires_at",columnList="expires_at")})
public class OAuthToken {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="authorization_id",nullable=false) private OAuthAuthorization authorization;
    @Column(name="token_type",nullable=false,length=32) private String tokenType;
    @Column(nullable=false,columnDefinition="text") private String value;
    @Column(name="value_hash",nullable=false,length=64) private String valueHash;
    @Column(length=64) private String jti;
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(nullable=false,columnDefinition="text[]") private String[] scopes=new String[0];
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private Map<String,Object> metadata;
    @Column(name="issued_at",nullable=false) private Instant issuedAt;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(nullable=false) private boolean invalidated;
    @Column(name="invalidated_at") private Instant invalidatedAt;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="rotated_to_id") private OAuthToken rotatedTo;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    public OAuthToken(){}
    public UUID getId(){return id;} public OAuthAuthorization getAuthorization(){return authorization;} public String getTokenType(){return tokenType;} public String getValue(){return value;} public String getValueHash(){return valueHash;} public String getJti(){return jti;} public String[] getScopes(){return scopes;} public Map<String,Object> getMetadata(){return metadata;} public Instant getIssuedAt(){return issuedAt;} public Instant getExpiresAt(){return expiresAt;} public boolean isInvalidated(){return invalidated;} public Instant getInvalidatedAt(){return invalidatedAt;} public OAuthToken getRotatedTo(){return rotatedTo;}
    public void setId(UUID v){id=v;} public void setCreatedAt(Instant v){createdAt=v;} public void setAuthorization(OAuthAuthorization v){authorization=v;} public void setTokenType(String v){tokenType=v;} public void setValue(String v){value=v;} public void setValueHash(String v){valueHash=v;} public void setJti(String v){jti=v;} public void setScopes(String[] v){scopes=v;} public void setMetadata(Map<String,Object> v){metadata=v;} public void setIssuedAt(Instant v){issuedAt=v;} public void setExpiresAt(Instant v){expiresAt=v;} public void setInvalidated(boolean v){invalidated=v;} public void setInvalidatedAt(Instant v){invalidatedAt=v;} public void setRotatedTo(OAuthToken v){rotatedTo=v;}
    public void invalidate(){invalidated=true;invalidatedAt=Instant.now();}
}


