package io.github.inarazahin.employees.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.matchesPattern;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Authentication and authorization")
class AuthApiTest extends ApiTestBase {

    @Test
    @DisplayName("valid credentials return a signed bearer token")
    void loginReturnsToken() {
        anonymous().body(Map.of("username", ADMIN, "password", ADMIN_PASSWORD))
                .post("/api/v1/auth/login")
                .then().statusCode(200)
                .body("tokenType", equalTo("Bearer"))
                .body("expiresIn", equalTo(3600))
                .body("accessToken", matchesPattern("^[\\w-]+\\.[\\w-]+\\.[\\w-]+$"));
    }

    @Test
    @DisplayName("a wrong password is rejected with 401 and no token")
    void wrongPassword() {
        anonymous().body(Map.of("username", ADMIN, "password", "not-the-password"))
                .post("/api/v1/auth/login")
                .then().statusCode(401)
                .contentType("application/problem+json")
                .body("detail", equalTo("Invalid username or password"))
                .body("$", org.hamcrest.Matchers.not(hasKey("accessToken")));
    }

    @Test
    @DisplayName("an unknown user gets the same 401 as a wrong password (no user enumeration)")
    void unknownUser() {
        anonymous().body(Map.of("username", "nobody", "password", "whatever"))
                .post("/api/v1/auth/login")
                .then().statusCode(401)
                .body("detail", equalTo("Invalid username or password"));
    }

    @Test
    @DisplayName("blank credentials fail validation")
    void blankCredentials() {
        anonymous().body(Map.of("username", "", "password", ""))
                .post("/api/v1/auth/login")
                .then().statusCode(400)
                .body("errors", hasKey("username"))
                .body("errors", hasKey("password"));
    }

    @Test
    @DisplayName("requests without a token are 401")
    void missingToken() {
        anonymous().get("/api/v1/employees")
                .then().statusCode(401)
                .contentType("application/problem+json")
                .body("title", equalTo("Unauthorized"));
    }

    @Test
    @DisplayName("a tampered token is rejected")
    void tamperedToken() {
        String token = token(ADMIN, ADMIN_PASSWORD);
        String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");

        anonymous().auth().oauth2(tampered)
                .get("/api/v1/employees")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("a viewer can read but not write")
    void viewerIsReadOnly() {
        long department = createDepartment();

        asViewer().get("/api/v1/employees").then().statusCode(200);
        asViewer().body(employee(department))
                .post("/api/v1/employees")
                .then().statusCode(403)
                .contentType("application/problem+json")
                .body("title", equalTo("Forbidden"));
        asViewer().delete("/api/v1/departments/" + department).then().statusCode(403);
    }
}
