package io.github.inarazahin.employees.api;

import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Black-box API tests: the full application runs on a random port against a real PostgreSQL
 * in Docker (Testcontainers), and tests talk to it over HTTP with RestAssured - exactly like a
 * client would. One container and one application context are shared by all API test classes.
 *
 * <p>Tests never depend on each other: each creates its own departments and employees with
 * unique names, so they can run in any order and against a database that already has data.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(ApiTestBase.Containers.class)
abstract class ApiTestBase {

    static final String ADMIN = "admin";
    static final String ADMIN_PASSWORD = "Admin-Pass-2024!";
    static final String VIEWER = "viewer";
    static final String VIEWER_PASSWORD = "Viewer-Pass-2024!";

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:17-alpine");
        }
    }

    private static final Map<String, String> TOKENS = new HashMap<>();

    @LocalServerPort
    int port;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // ------------------------------------------------------------------ clients

    RequestSpecification anonymous() {
        return given().contentType(ContentType.JSON).accept(ContentType.JSON);
    }

    RequestSpecification asAdmin() {
        return anonymous().auth().oauth2(token(ADMIN, ADMIN_PASSWORD));
    }

    RequestSpecification asViewer() {
        return anonymous().auth().oauth2(token(VIEWER, VIEWER_PASSWORD));
    }

    String token(String username, String password) {
        // Tokens are cached per port: each application context has its own users table.
        return TOKENS.computeIfAbsent(port + ":" + username, key -> anonymous()
                .body(Map.of("username", username, "password", password))
                .post("/api/v1/auth/login")
                .then().statusCode(200)
                .extract().path("accessToken"));
    }

    // ------------------------------------------------------------------ test data

    static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    long createDepartment() {
        return createDepartment(unique("Dept"));
    }

    long createDepartment(String name) {
        return ((Number) asAdmin()
                .body(Map.of("name", name, "description", "Created by API tests"))
                .post("/api/v1/departments")
                .then().statusCode(201)
                .extract().path("id")).longValue();
    }

    /** A valid employee payload; tests tweak individual fields to probe validation. */
    static Map<String, Object> employee(long departmentId) {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", "Ada");
        body.put("lastName", "Lovelace");
        body.put("email", "ada." + tag + "@example.com");
        body.put("jobTitle", "Analyst");
        body.put("salary", new BigDecimal("75000.00"));
        body.put("hireDate", LocalDate.now().minusYears(1).toString());
        body.put("departmentId", departmentId);
        return body;
    }

    long createEmployee(Map<String, Object> body) {
        return ((Number) asAdmin().body(body)
                .post("/api/v1/employees")
                .then().statusCode(201)
                .extract().path("id")).longValue();
    }
}
