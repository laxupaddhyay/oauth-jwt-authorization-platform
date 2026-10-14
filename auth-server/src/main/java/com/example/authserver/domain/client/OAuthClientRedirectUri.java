package com.example.authserver.domain.client;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "oauth_client_redirect_uri")
public class OAuthClientRedirectUri {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "client_pk", nullable = false) private OAuthClient client;
    @Column(nullable = false, length = 2048) private String uri;
    @Column(name = "uri_type", nullable = false, length = 24) private String uriType = "REDIRECT";
    protected OAuthClientRedirectUri() {}
    public OAuthClientRedirectUri(String uri, String uriType) { this.uri=uri; this.uriType=uriType; }
    public UUID getId(){return id;} public OAuthClient getClient(){return client;} public String getUri(){return uri;} public String getUriType(){return uriType;} public void setClient(OAuthClient v){client=v;}
}
