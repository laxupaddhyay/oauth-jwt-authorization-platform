package com.example.authserver.web.admin.dto;
import jakarta.validation.constraints.Email; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Size; import java.util.Set;
public record UserCreateRequest(@NotBlank @Size(max=128) String username,@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=12,max=128) String password,Set<String> roles) {}
