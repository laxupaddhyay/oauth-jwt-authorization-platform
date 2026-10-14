package com.example.authserver.repository;
import com.example.authserver.domain.client.OAuthScope;
import java.util.List; import org.springframework.data.jpa.repository.JpaRepository;
public interface OAuthScopeRepository extends JpaRepository<OAuthScope, String> { List<OAuthScope> findAllByNameIn(Iterable<String> names); }
