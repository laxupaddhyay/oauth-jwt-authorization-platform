package com.example.authserver.service.user;

import com.example.authserver.domain.user.AppUser;
import com.example.authserver.repository.AppRoleRepository;
import com.example.authserver.repository.AppUserRepository;
import com.example.authserver.web.admin.dto.UserCreateRequest;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminService {
    private final AppUserRepository users; private final AppRoleRepository roles; private final PasswordEncoder passwords;
    public UserAdminService(AppUserRepository users,AppRoleRepository roles,PasswordEncoder passwords){this.users=users;this.roles=roles;this.passwords=passwords;}
    @Transactional public AppUser create(UserCreateRequest request){
        if(users.existsByUsername(request.username())||users.existsByEmail(request.email()))throw new IllegalArgumentException("Username or email is already registered");
        Set<String> requested=request.roles()==null||request.roles().isEmpty()?Set.of("ROLE_USER"):request.roles();var found=roles.findByNameIn(requested);
        if(found.size()!=requested.size())throw new IllegalArgumentException("One or more roles are unknown");
        var user=new AppUser(request.username().trim(),request.email().trim(),passwords.encode(request.password()));user.setRoles(found.stream().collect(Collectors.toSet()));return users.save(user);
    }
    @Transactional public void disable(String username){var user=find(username);user.setEnabled(false);users.save(user);}
    @Transactional public void unlock(String username){var user=find(username);user.setAccountNonLocked(true);user.setFailedLoginAttempts(0);user.setLockedUntil(null);users.save(user);}
    @Transactional public void assignRoles(String username,Set<String> names){var user=find(username);var found=roles.findByNameIn(names);if(found.size()!=names.size())throw new IllegalArgumentException("One or more roles are unknown");user.setRoles(found.stream().collect(Collectors.toSet()));users.save(user);}
    @Transactional public void resetPassword(String username,String password){if(password==null||password.length()<12||password.length()>128)throw new IllegalArgumentException("Password must be between 12 and 128 characters");var user=find(username);user.setPasswordHash(passwords.encode(password));user.setFailedLoginAttempts(0);user.setLockedUntil(null);user.setAccountNonLocked(true);users.save(user);}
    private AppUser find(String username){return users.findByUsernameForUpdate(username).orElseThrow(UserNotFoundException::new);}
    public static class UserNotFoundException extends RuntimeException{public UserNotFoundException(){super("User not found");}}
}
