package io.github.inarazahin.employees.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the configured demo accounts on startup if they don't exist yet. */
@Component
public class DemoUsers implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUsers.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;

    public DemoUsers(AppUserRepository users, PasswordEncoder passwordEncoder, AuthProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (properties.demoUsers() == null) {
            return;
        }
        properties.demoUsers().forEach((username, spec) -> {
            if (users.findByUsername(username).isPresent()) {
                return;
            }
            String[] parts = spec.split(":", 2);
            AppUser.Role role = AppUser.Role.valueOf(parts[0].trim().toUpperCase());
            users.save(new AppUser(username, passwordEncoder.encode(parts[1]), role));
            log.info("Created demo user '{}' with role {}", username, role);
        });
    }
}
