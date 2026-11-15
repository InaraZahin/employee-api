package io.github.inarazahin.employees.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Documentation and health")
class PlatformApiTest extends ApiTestBase {

    @Test
    @DisplayName("the OpenAPI document describes every endpoint and the bearer scheme")
    void openApi() {
        anonymous().get("/v3/api-docs")
                .then().statusCode(200)
                .body("paths", hasKey("/api/v1/employees"))
                .body("paths", hasKey("/api/v1/employees/{id}"))
                .body("paths", hasKey("/api/v1/departments"))
                .body("paths", hasKey("/api/v1/auth/login"))
                .body("components.securitySchemes.bearer.scheme", equalTo("bearer"));
    }

    @Test
    @DisplayName("Swagger UI is served without authentication")
    void swaggerUi() {
        anonymous().accept("text/html").redirects().follow(true)
                .get("/swagger-ui.html")
                .then().statusCode(200);
    }

    @Test
    @DisplayName("the health endpoint reports UP")
    void health() {
        anonymous().get("/actuator/health")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }
}
