package com.example.authserver.repository;
import com.example.authserver.domain.user.AppRole;
import java.util.Collection; import java.util.List; import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AppRoleRepository extends JpaRepository<AppRole, UUID> { Optional<AppRole> findByName(String name); List<AppRole> findByNameIn(Collection<String> names); }
