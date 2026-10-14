package com.example.authserver.repository;
import com.example.authserver.domain.authorization.OAuthAuthorization;
import java.util.List; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
public interface OAuthAuthorizationRepository extends JpaRepository<OAuthAuthorization, UUID> {
    List<OAuthAuthorization> findByFamilyId(UUID familyId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) List<OAuthAuthorization> findLockedByFamilyId(UUID familyId);
}
