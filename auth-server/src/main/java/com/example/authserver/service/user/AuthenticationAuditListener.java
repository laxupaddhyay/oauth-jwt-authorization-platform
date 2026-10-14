package com.example.authserver.service.user;

import com.example.authserver.domain.audit.AuditEvent;
import com.example.authserver.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAuditListener {
    private final LoginAttemptService loginAttempts;
    private final AuditEventRepository audit;
    public AuthenticationAuditListener(LoginAttemptService loginAttempts,AuditEventRepository audit){this.loginAttempts=loginAttempts;this.audit=audit;}
    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event){
        String username=event.getAuthentication().getName();
        loginAttempts.recordFailure(username);
        audit.save(new AuditEvent("LOGIN_FAILURE",username,false,JsonNodeFactory.instance.objectNode().put("reason","invalid_credentials")));
    }
    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event){
        String username=event.getAuthentication().getName();
        loginAttempts.recordSuccess(username);
        audit.save(new AuditEvent("LOGIN_SUCCESS",username,true,JsonNodeFactory.instance.objectNode()));
    }
}

