package com.example.authserver.repository;
import com.example.authserver.domain.authorization.OAuthToken;
import java.time.Instant; import java.util.List; import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Modifying; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param;
public interface OAuthTokenRepository extends JpaRepository<OAuthToken, UUID> {
    Optional<OAuthToken> findByTokenTypeAndValueHash(String tokenType,String valueHash);
    Optional<OAuthToken> findByValueHash(String valueHash);
    List<OAuthToken> findByAuthorizationId(UUID authorizationId);
    @Query("select t from OAuthToken t where t.authorization.familyId = :familyId") List<OAuthToken> findByFamilyId(@Param("familyId") UUID familyId);
    @Modifying @Query(value="update oauth_token set invalidated = true, invalidated_at = now() where authorization_id in (select id from oauth_authorization where family_id = :familyId)",nativeQuery=true) int invalidateFamily(@Param("familyId") UUID familyId);
    @Modifying @Query(value="delete from oauth_token where id in (select id from oauth_token where (expires_at < :cutoff or (invalidated = true and invalidated_at < :invalidCutoff)) order by expires_at limit :batchSize)",nativeQuery=true) int deleteExpiredBatch(@Param("cutoff") Instant cutoff,@Param("invalidCutoff") Instant invalidCutoff,@Param("batchSize") int batchSize);
}

