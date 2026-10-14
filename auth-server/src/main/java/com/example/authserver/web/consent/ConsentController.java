package com.example.authserver.web.consent;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ConsentController {
    @GetMapping("/oauth2/consent")
    public String consent(@RequestParam(name = "client_id", defaultValue = "Application") String clientId,
                          @RequestParam(name = "scope", defaultValue = "") String scope,
                          @RequestParam(name = "state", defaultValue = "") String state,
                          Model model) {
        List<String> scopes = Arrays.stream(scope.trim().split("\\s+"))
            .filter(value -> !value.isBlank()).distinct().toList();
        model.addAttribute("clientId", clientId);
        model.addAttribute("state", state);
        model.addAttribute("scopes", scopes);
        return "consent";
    }
}
