package com.example.authserver.domain.client;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "oauth_scope")
public class OAuthScope {
    @Id @Column(length = 100) private String name;
    @Column(nullable = false, length = 255) private String description;
    @Column(name = "is_openid", nullable = false) private boolean openid;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth_scope_claim", joinColumns = @JoinColumn(name = "scope_name"))
    @Column(name = "claim_name", nullable = false, length = 100)
    private Set<String> claimNames = new HashSet<>();
    protected OAuthScope() {}
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isOpenid() { return openid; }
    public Set<String> getClaimNames() { return claimNames; }
}

