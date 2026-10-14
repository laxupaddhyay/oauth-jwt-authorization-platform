package com.example.authserver.domain.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true, columnDefinition = "citext") private String username;
    @Column(nullable = false, unique = true, columnDefinition = "citext") private String email;
    @Column(name = "email_verified", nullable = false) private boolean emailVerified;
    @Column(name = "password_hash", nullable = false, length = 255) private String passwordHash;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "account_non_locked", nullable = false) private boolean accountNonLocked = true;
    @Column(name = "failed_login_attempts", nullable = false) private int failedLoginAttempts;
    @Column(name = "locked_until") private Instant lockedUntil;
    @Column(name = "password_changed_at", nullable = false) private Instant passwordChangedAt = Instant.now();
    @Column(length = 255) private String name;
    @Column(name = "given_name", length = 128) private String givenName;
    @Column(name = "family_name", length = 128) private String familyName;
    @Column(name = "preferred_username", length = 128) private String preferredUsername;
    @Column(length = 1024) private String picture;
    @Column(length = 16) private String locale;
    @Column(length = 64) private String zoneinfo;
    @Column(name = "phone_number", length = 32) private String phoneNumber;
    @Column(name = "phone_number_verified", nullable = false) private boolean phoneNumberVerified;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "extra_claims", nullable = false, columnDefinition = "jsonb") private JsonNode extraClaims = JsonNodeFactory.instance.objectNode();
    @Column(name = "last_login_at") private Instant lastLoginAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    @Version @Column(nullable = false) private long version;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "app_user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<AppRole> roles = new HashSet<>();

    protected AppUser() {}
    public AppUser(String username, String email, String passwordHash) { this.username = username; this.email = email; this.passwordHash = passwordHash; }
    @PreUpdate void beforeUpdate() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public boolean isEmailVerified() { return emailVerified; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }
    public boolean isAccountNonLocked() { return lockedUntil != null ? !lockedUntil.isAfter(Instant.now()) : accountNonLocked; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public String getName() { return name; }
    public String getGivenName() { return givenName; }
    public String getFamilyName() { return familyName; }
    public String getPreferredUsername() { return preferredUsername == null ? username : preferredUsername; }
    public String getPicture() { return picture; }
    public String getLocale() { return locale; }
    public String getZoneinfo() { return zoneinfo; }
    public String getPhoneNumber() { return phoneNumber; }
    public boolean isPhoneNumberVerified() { return phoneNumberVerified; }
    public JsonNode getExtraClaims() { return extraClaims; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Set<AppRole> getRoles() { return roles; }
    public long getVersion() { return version; }
    public void setEmailVerified(boolean value) { emailVerified = value; }
    public void setEnabled(boolean value) { enabled = value; }
    public void setAccountNonLocked(boolean value) { accountNonLocked = value; }
    public void setPasswordHash(String value) { passwordHash = value; passwordChangedAt = Instant.now(); }
    public void setFailedLoginAttempts(int value) { failedLoginAttempts = value; }
    public void setLockedUntil(Instant value) { lockedUntil = value; }
    public void setLastLoginAt(Instant value) { lastLoginAt = value; }
    public void setRoles(Set<AppRole> value) { roles = value; }
}




