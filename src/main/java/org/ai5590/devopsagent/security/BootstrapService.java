package org.ai5590.devopsagent.security;

import org.ai5590.devopsagent.config.AppConfig;
import org.ai5590.devopsagent.config.ConfigLoader;
import org.ai5590.devopsagent.db.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class BootstrapService {
    private static final Logger log = LoggerFactory.getLogger(BootstrapService.class);
    private final ConfigLoader configLoader;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapService(ConfigLoader configLoader, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.configLoader = configLoader;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        AppConfig config = configLoader.getConfig();
        String mode = config.getBootstrapUsersMode();
        log.info("Bootstrapping users in mode: {}", mode);

        for (AppConfig.BootstrapUser user : config.getBootstrapUsers()) {
            String hash = passwordEncoder.encode(user.getPassword());
            if ("UPSERT".equalsIgnoreCase(mode)) {
                userRepository.upsertUser(user.getLogin(), hash);
            } else if ("CREATE_ONLY".equalsIgnoreCase(mode)) {
                if (!userRepository.existsByLogin(user.getLogin())) {
                    userRepository.createUser(user.getLogin(), hash);
                } else {
                    log.info("User {} already exists, skipping (CREATE_ONLY mode)", user.getLogin());
                }
            }
        }
        log.info("Bootstrap completed");
    }
}
