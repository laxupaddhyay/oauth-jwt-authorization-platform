package com.example.authserver.domain.authorization;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="oauth_authorization_consent")
@IdClass(OAuthAuthorizationConsent.Key.class)
public class OAuthAuthorizationConsent {
    @Id @Column(name="client_pk",nullable=false) private UUID clientPk;
    @Id @Column(name="principal_name",nullable=false,length=200) private String principalName;
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(nullable=false,columnDefinition="text[]") private String[] authorities=new String[0];
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
    public OAuthAuthorizationConsent(){}
    public UUID getClientPk(){return clientPk;} public String getPrincipalName(){return principalName;} public String[] getAuthorities(){return authorities;}
    public void setClientPk(UUID v){clientPk=v;} public void setPrincipalName(String v){principalName=v;} public void setAuthorities(String[] v){authorities=v;}
    @PreUpdate void touch(){updatedAt=Instant.now();}
    public static class Key implements Serializable {
        public UUID clientPk; public String principalName;
        public Key(){}
        public Key(UUID clientPk,String principalName){this.clientPk=clientPk;this.principalName=principalName;}
        @Override public boolean equals(Object o){return o instanceof Key k&&Objects.equals(clientPk,k.clientPk)&&Objects.equals(principalName,k.principalName);}
        @Override public int hashCode(){return Objects.hash(clientPk,principalName);}
    }
}

