package io.github.inarazahin.employees.auth;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param jwtSecret  base64-encoded HMAC key (at least 256 bits); set JWT_SECRET in any real deployment
 * @param tokenTtl   lifetime of issued access tokens
 * @param demoUsers  accounts created at startup when missing: username -> "ROLE:password"
 */
@ConfigurationProperties("app.auth")
public record AuthProperties(String jwtSecret, Duration tokenTtl, Map<String, String> demoUsers) {
}
