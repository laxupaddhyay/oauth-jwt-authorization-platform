package com.example.authserver.validation;

import com.example.authserver.web.admin.dto.ClientCreateRequest;

public interface ClientRegistrationRule {
    void validate(ClientCreateRequest request);
}
