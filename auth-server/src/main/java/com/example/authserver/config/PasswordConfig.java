package com.example.authserver.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {
    @Bean
    PasswordEncoder passwordEncoder(){
        PasswordEncoder bcrypt=new BCryptPasswordEncoder(12);
        Map<String,PasswordEncoder> encoders=new HashMap<>();encoders.put("bcrypt",bcrypt);
        return new DelegatingPasswordEncoder("bcrypt",encoders);
    }
}
