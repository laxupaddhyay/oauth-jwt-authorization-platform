package com.example.authserver.repository;
import com.example.authserver.domain.audit.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditEventRepository extends JpaRepository<AuditEvent,Long> { }
