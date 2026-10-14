package com.example.authserver.service.key;

import com.example.authserver.config.AuthServerProperties;
import com.example.authserver.domain.key.SigningKey;
import com.example.authserver.repository.SigningKeyRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KeyRotationService {
    private static final long KEY_LOCK=821067351204L;
    private final SigningKeyRepository keys; private final KeyGenerator generator; private final KeyEncryptor encryptor; private final JdbcTemplate jdbc; private final AuthServerProperties properties;
    public KeyRotationService(SigningKeyRepository keys,KeyGenerator generator,KeyEncryptor encryptor,JdbcTemplate jdbc,AuthServerProperties properties){this.keys=keys;this.generator=generator;this.encryptor=encryptor;this.jdbc=jdbc;this.properties=properties;}
    @Scheduled(cron="${authserver.keys.rotation-cron:0 0 3 * * *}")
    @net.javacrumbs.shedlock.spring.annotation.SchedulerLock(name="signing-key-rotation")
    @Transactional
    public void rotateIfDue(){
        jdbc.execute("select pg_advisory_xact_lock("+KEY_LOCK+")");
        var active=keys.findLockedActive().orElse(null);if(active==null){createActive();return;}
        Instant now=Instant.now();if(active.getCreatedAt().plus(properties.keys().rotationInterval()).isAfter(now)){retireExpired(now);return;}
        active.setStatus("RETIRING");active.setRetireAt(now);active.setExpiresAt(now.plus(properties.tokens().refreshTokenTtl()).plus(properties.keys().retireGracePeriod()));
        keys.saveAndFlush(active);createActive();retireExpired(now);
    }
    private void createActive(){var pair=generator.generate();String kid=UUID.randomUUID().toString().replace("-","");String pem="-----BEGIN PUBLIC KEY-----\n"+java.util.Base64.getMimeEncoder(64,new byte[]{'\n'}).encodeToString(pair.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----";var entity=new SigningKey(kid,"RS256","RSA",pem,encryptor.encrypt(pair.getPrivate().getEncoded()));entity.setStatus("ACTIVE");entity.setActivatedAt(Instant.now());keys.saveAndFlush(entity);}
    private void retireExpired(Instant now){for(SigningKey key:keys.findByStatusIn(java.util.List.of("RETIRING")))if(key.getExpiresAt()!=null&&!key.getExpiresAt().isAfter(now))key.setStatus("RETIRED");}
}

