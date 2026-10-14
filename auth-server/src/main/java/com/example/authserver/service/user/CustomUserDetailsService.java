package com.example.authserver.service.user;

import com.example.authserver.domain.user.AppUser;
import com.example.authserver.repository.AppUserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private static final String INVALID_CREDENTIALS="Invalid username or password";
    private final AppUserRepository users;
    public CustomUserDetailsService(AppUserRepository users){this.users=users;}
    @Override @Transactional(readOnly=true)
    public UserDetails loadUserByUsername(String username){
        AppUser user=(username!=null&&username.contains("@")?users.findWithRolesByEmail(username):users.findWithRolesByUsername(username)).orElseThrow(()->new UsernameNotFoundException(INVALID_CREDENTIALS));
        var authorities=user.getRoles().stream().map(role->new SimpleGrantedAuthority(role.getName())).toList();
        return User.withUsername(user.getUsername()).password(user.getPasswordHash()).authorities(authorities)
            .disabled(!user.isEnabled()).accountLocked(!user.isAccountNonLocked()).build();
    }
}

