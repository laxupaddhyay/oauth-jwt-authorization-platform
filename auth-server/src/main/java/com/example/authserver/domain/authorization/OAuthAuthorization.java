package com.example.authserver.domain.authorization;

import com.example.authserver.domain.client.OAuthClient;
import com.example.authserver.domain.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "oauth_authorization", indexes = {@Index(name="idx_authz_client_principal",columnList="client_pk,principal_name"),@Index(name="idx_authz_family",columnList="family_id")})
public class OAuthAuthorization {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name="client_pk",nullable=false) private OAuthClient client;
    @Column(name="principal_name",nullable=false,length=200) private String principalName;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="user_id") private AppUser user;
    @Column(name="grant_type",nullable=false,length=64) private String grantType;
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(name="authorized_scopes",nullable=false,columnDefinition="text[]") private String[] authorizedScopes = new String[0];
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private Map<String,Object> attributes;
    @Column(name="state_hash",length=64) private String stateHash;
    @Column(name="family_id",nullable=false) private UUID familyId=UUID.randomUUID();
    @Column(nullable=false) private boolean revoked;
    @Column(name="revoked_at") private Instant revokedAt;
    @Column(name="revoked_reason",length=100) private String revokedReason;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
    @Version @Column(nullable=false) private long version;
    @OneToMany(mappedBy="authorization",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) private List<OAuthToken> tokens=new ArrayList<>();
    public OAuthAuthorization(){}
    public UUID getId(){return id;} public void setId(UUID v){id=v;} public void setCreatedAt(Instant v){createdAt=v;} public OAuthClient getClient(){return client;} public String getPrincipalName(){return principalName;} public AppUser getUser(){return user;} public String getGrantType(){return grantType;} public String[] getAuthorizedScopes(){return authorizedScopes;} public Map<String,Object> getAttributes(){return attributes;} public String getStateHash(){return stateHash;} public UUID getFamilyId(){return familyId;} public boolean isRevoked(){return revoked;} public Instant getRevokedAt(){return revokedAt;} public String getRevokedReason(){return revokedReason;} public Instant getCreatedAt(){return createdAt;} public long getVersion(){return version;} public List<OAuthToken> getTokens(){return tokens;}
    public void setClient(OAuthClient v){client=v;} public void setPrincipalName(String v){principalName=v;} public void setUser(AppUser v){user=v;} public void setGrantType(String v){grantType=v;} public void setAuthorizedScopes(String[] v){authorizedScopes=v;} public void setAttributes(Map<String,Object> v){attributes=v;} public void setStateHash(String v){stateHash=v;} public void setFamilyId(UUID v){familyId=v;} public void setRevoked(boolean v){revoked=v;} public void setRevokedAt(Instant v){revokedAt=v;} public void setRevokedReason(String v){revokedReason=v;} public void setTokens(List<OAuthToken> v){tokens.clear();for(var token:v){token.setAuthorization(this);tokens.add(token);}}
    public void revoke(String reason){revoked=true;revokedAt=Instant.now();revokedReason=reason;updatedAt=Instant.now();}
    @PreUpdate void touch(){updatedAt=Instant.now();}
}


