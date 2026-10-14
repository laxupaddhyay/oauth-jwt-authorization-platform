package com.example.authserver.domain.client;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "oauth_client")
public class OAuthClient {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "client_id", nullable = false, unique = true, length = 100) private String clientId;
    @Column(name = "client_secret_hash", length = 255) private String clientSecretHash;
    @Column(name = "client_id_issued_at", nullable = false) private Instant clientIdIssuedAt = Instant.now();
    @Column(name = "client_secret_expires_at") private Instant clientSecretExpiresAt;
    @Column(name = "client_name", nullable = false, length = 200) private String clientName;
    @Column(name = "client_type", nullable = false, length = 16) private String clientType;
    @Column(name = "require_pkce", nullable = false) private boolean requirePkce = true;
    @Column(name = "require_consent", nullable = false) private boolean requireConsent = true;
    @Column(name = "access_token_format", nullable = false, length = 16) private String accessTokenFormat = "SELF_CONTAINED";
    @Column(name = "access_token_ttl_seconds", nullable = false) private int accessTokenTtlSeconds = 600;
    @Column(name = "refresh_token_ttl_seconds", nullable = false) private int refreshTokenTtlSeconds = 1209600;
    @Column(name = "authorization_code_ttl_seconds", nullable = false) private int authorizationCodeTtlSeconds = 300;
    @Column(name = "reuse_refresh_tokens", nullable = false) private boolean reuseRefreshTokens;
    @Column(name = "id_token_signature_alg", nullable = false, length = 16) private String idTokenSignatureAlg = "RS256";
    @Column(nullable = false) private boolean enabled = true;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "extra_settings", nullable = false, columnDefinition = "jsonb") private java.util.Map<String,Object> extraSettings = new java.util.HashMap<>();
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    @Version @Column(nullable = false) private long version;
    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "oauth_client_auth_method", joinColumns = @JoinColumn(name = "client_pk")) @Column(name = "method", nullable = false, length = 64) private Set<String> authMethods = new HashSet<>();
    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "oauth_client_grant_type", joinColumns = @JoinColumn(name = "client_pk")) @Column(name = "grant_type", nullable = false, length = 64) private Set<String> grantTypes = new HashSet<>();
    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "oauth_client_scope", joinColumns = @JoinColumn(name = "client_pk")) @Column(name = "scope_name", nullable = false, length = 100) private Set<String> scopes = new HashSet<>();
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER) private Set<OAuthClientRedirectUri> redirectUris = new HashSet<>();

    public OAuthClient() {}
    public UUID getId(){return id;} public void setId(UUID v){id=v;} public void setCreatedAt(Instant v){createdAt=v;} public String getClientId(){return clientId;} public String getClientSecretHash(){return clientSecretHash;} public Instant getClientIdIssuedAt(){return clientIdIssuedAt;} public Instant getClientSecretExpiresAt(){return clientSecretExpiresAt;} public String getClientName(){return clientName;} public String getClientType(){return clientType;} public boolean isRequirePkce(){return requirePkce;} public boolean isRequireConsent(){return requireConsent;} public String getAccessTokenFormat(){return accessTokenFormat;} public int getAccessTokenTtlSeconds(){return accessTokenTtlSeconds;} public int getRefreshTokenTtlSeconds(){return refreshTokenTtlSeconds;} public int getAuthorizationCodeTtlSeconds(){return authorizationCodeTtlSeconds;} public boolean isReuseRefreshTokens(){return reuseRefreshTokens;} public String getIdTokenSignatureAlg(){return idTokenSignatureAlg;} public boolean isEnabled(){return enabled;} public java.util.Map<String,Object> getExtraSettings(){return extraSettings;} public String getCreatedBy(){return createdBy;} public Instant getCreatedAt(){return createdAt;} public Set<String> getAuthMethods(){return authMethods;} public Set<String> getGrantTypes(){return grantTypes;} public Set<String> getScopes(){return scopes;} public Set<OAuthClientRedirectUri> getRedirectUris(){return redirectUris;}
    public void setClientId(String v){clientId=v;} public void setClientSecretHash(String v){clientSecretHash=v;} public void setClientIdIssuedAt(Instant v){clientIdIssuedAt=v;} public void setClientSecretExpiresAt(Instant v){clientSecretExpiresAt=v;} public void setClientName(String v){clientName=v;} public void setClientType(String v){clientType=v;} public void setRequirePkce(boolean v){requirePkce=v;} public void setRequireConsent(boolean v){requireConsent=v;} public void setAccessTokenFormat(String v){accessTokenFormat=v;} public void setAccessTokenTtlSeconds(int v){accessTokenTtlSeconds=v;} public void setRefreshTokenTtlSeconds(int v){refreshTokenTtlSeconds=v;} public void setAuthorizationCodeTtlSeconds(int v){authorizationCodeTtlSeconds=v;} public void setReuseRefreshTokens(boolean v){reuseRefreshTokens=v;} public void setIdTokenSignatureAlg(String v){idTokenSignatureAlg=v;} public void setEnabled(boolean v){enabled=v;} public void setExtraSettings(java.util.Map<String,Object> v){extraSettings=v;} public void setCreatedBy(String v){createdBy=v;} public void setAuthMethods(Set<String> v){authMethods=v;} public void setGrantTypes(Set<String> v){grantTypes=v;} public void setScopes(Set<String> v){scopes=v;} public void setRedirectUris(Set<OAuthClientRedirectUri> v){redirectUris.clear(); for(var r:v){r.setClient(this);redirectUris.add(r);}}
    @PreUpdate void touch(){updatedAt=Instant.now();}
}



