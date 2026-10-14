package com.example.authserver.web.admin;

import com.example.authserver.domain.user.AppUser;
import com.example.authserver.service.user.UserAdminService;
import com.example.authserver.web.admin.dto.UserCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/admin/users") @Validated
public class UserAdminController {
    private final UserAdminService service;
    public UserAdminController(UserAdminService service){this.service=service;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public UserView create(@Valid @RequestBody UserCreateRequest request){var user=service.create(request);return view(user);}
    @PostMapping("/{username}/disable") @ResponseStatus(HttpStatus.NO_CONTENT) public void disable(@PathVariable String username){service.disable(username);}
    @PostMapping("/{username}/unlock") @ResponseStatus(HttpStatus.NO_CONTENT) public void unlock(@PathVariable String username){service.unlock(username);}
    @PutMapping("/{username}/roles") @ResponseStatus(HttpStatus.NO_CONTENT) public void roles(@PathVariable String username,@RequestBody Set<String> roleNames){service.assignRoles(username,roleNames);}
    @PutMapping("/{username}/password") @ResponseStatus(HttpStatus.NO_CONTENT) public void password(@PathVariable String username,@Valid @RequestBody PasswordRequest request){service.resetPassword(username,request.password());}
    private UserView view(AppUser user){return new UserView(user.getId(),user.getUsername(),user.getEmail(),user.isEnabled(),user.getRoles().stream().map(r->r.getName()).collect(java.util.stream.Collectors.toSet()));}
    public record UserView(java.util.UUID id,String username,String email,boolean enabled,Set<String> roles){}
    public record PasswordRequest(@NotBlank @Size(min=12,max=128) String password){}
}
