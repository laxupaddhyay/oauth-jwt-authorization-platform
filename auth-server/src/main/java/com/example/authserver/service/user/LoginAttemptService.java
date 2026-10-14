package com.example.authserver.service.user;

import com.example.authserver.repository.AppUserRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {
    private static final int MAX_FAILURES=5; private static final long LOCK_SECONDS=15*60;
    private final AppUserRepository users;
    public LoginAttemptService(AppUserRepository users){this.users=users;}
    @Transactional public void recordFailure(String username){
        findForUpdate(username).ifPresent(user->{
            Instant now=Instant.now();
            if(user.getLockedUntil()!=null&&user.getLockedUntil().isAfter(now))return;
            if(user.getLockedUntil()!=null&&!user.getLockedUntil().isAfter(now)){user.setLockedUntil(null);user.setAccountNonLocked(true);user.setFailedLoginAttempts(0);}
            int failures=user.getFailedLoginAttempts()+1;user.setFailedLoginAttempts(failures);
            if(failures>=MAX_FAILURES){user.setAccountNonLocked(false);user.setLockedUntil(now.plusSeconds(LOCK_SECONDS));}
            users.save(user);
        });
    }
    private java.util.Optional<com.example.authserver.domain.user.AppUser> findForUpdate(String value){return value!=null&&value.contains("@")?users.findByEmailForUpdate(value):users.findByUsernameForUpdate(value);}
    @Transactional public void recordSuccess(String username){findForUpdate(username).ifPresent(user->{user.setFailedLoginAttempts(0);user.setLockedUntil(null);user.setAccountNonLocked(true);user.setLastLoginAt(Instant.now());users.save(user);});}
}

