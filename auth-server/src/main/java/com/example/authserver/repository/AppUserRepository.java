package com.example.authserver.repository;

import com.example.authserver.domain.user.AppUser;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByUsername(String username);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "roles") Optional<AppUser> findWithRolesByUsername(String username);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "roles") Optional<AppUser> findWithRolesByEmail(String email);
    Optional<AppUser> findByEmail(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.email = :email")
    Optional<AppUser> findByEmailForUpdate(@Param("email") String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.username = :username")
    Optional<AppUser> findByUsernameForUpdate(@Param("username") String username);
}



