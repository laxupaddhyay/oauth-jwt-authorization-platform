package com.example.authserver.repository;
import com.example.authserver.domain.key.SigningKey;
import java.util.List; import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Lock; import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
public interface SigningKeyRepository extends JpaRepository<SigningKey,String> {
    Optional<SigningKey> findFirstByStatusOrderByCreatedAtDesc(String status);
    List<SigningKey> findByStatusIn(List<String> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select k from SigningKey k where k.status='ACTIVE'") Optional<SigningKey> findLockedActive();
}
