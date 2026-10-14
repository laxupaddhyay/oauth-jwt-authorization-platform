package com.example.authserver.web.admin;

import com.example.authserver.service.client.ClientAdminService;
import com.example.authserver.web.admin.dto.ClientCreateRequest;
import com.example.authserver.web.admin.dto.ClientResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/clients")
public class ClientAdminController {
    private final ClientAdminService service;
    public ClientAdminController(ClientAdminService service){this.service=service;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ClientResponse create(@Valid @RequestBody ClientCreateRequest request){return service.create(request);}
    @GetMapping public Page<ClientResponse> list(@RequestParam(required=false) String filter,Pageable pageable){return service.list(filter,pageable);}
    @GetMapping("/{clientId}") public ClientResponse get(@PathVariable String clientId){return service.get(clientId);}
    @DeleteMapping("/{clientId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String clientId){service.delete(clientId);}
    @PostMapping("/{clientId}/rotate-secret") public ClientResponse rotate(@PathVariable String clientId){return service.rotateSecret(clientId);}
}
