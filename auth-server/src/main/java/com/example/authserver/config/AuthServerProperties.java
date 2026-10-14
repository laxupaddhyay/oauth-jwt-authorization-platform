package com.example.authserver.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "authserver")
public record AuthServerProperties(
    @NotBlank String issuer,
    @Valid @NotNull Tokens tokens,
    @Valid @NotNull Keys keys,
    @Valid @NotNull Admin admin,
    @Valid @NotNull RateLimit rateLimit
) {
    public record Tokens(Duration accessTokenTtl, Duration refreshTokenTtl, Duration authorizationCodeTtl, Duration idTokenTtl) {}
    public record Keys(Duration rotationInterval, Duration retireGracePeriod, @NotBlank String encryptionSecret) {}
    public record Admin(@NotBlank String bootstrapToken) {}
    public record RateLimit(int loginPerMinute, int tokenPerMinute) {}
}


