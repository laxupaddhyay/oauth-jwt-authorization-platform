package com.example.authserver.domain.audit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="audit_event",indexes=@Index(name="idx_audit_type_created",columnList="event_type,created_at"))
public class AuditEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="event_type",nullable=false,length=64) private String eventType;
    @Column(length=200) private String principal;
    @Column(name="client_id",length=100) private String clientId;
    @Column(name="ip_address",length=64) private String ipAddress;
    @Column(name="user_agent",length=512) private String userAgent;
    @Column(nullable=false) private boolean success=true;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private JsonNode details;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected AuditEvent(){}
    public AuditEvent(String eventType,String principal,boolean success,JsonNode details){this.eventType=eventType;this.principal=principal;this.success=success;this.details=details;}
}

