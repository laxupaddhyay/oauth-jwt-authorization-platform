package com.example.authserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.Base64;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class PersistenceMigrationIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties){
        properties.add("spring.datasource.url",POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username",POSTGRES::getUsername);
        properties.add("spring.datasource.password",POSTGRES::getPassword);
        properties.add("authserver.issuer",()->"http://localhost:9000");
        properties.add("authserver.keys.encryption-secret",PersistenceMigrationIntegrationTest::testEncryptionKey);
        properties.add("authserver.admin.bootstrap-token",()->"integration-test-bootstrap-token");
    }
    private static String testEncryptionKey(){byte[] key=new byte[32];new SecureRandom().nextBytes(key);return Base64.getEncoder().encodeToString(key);}
    @Autowired Flyway flyway;
    @Test void migrationsRunAndJpaMappingsValidateAgainstPostgres(){assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("6");}
}
