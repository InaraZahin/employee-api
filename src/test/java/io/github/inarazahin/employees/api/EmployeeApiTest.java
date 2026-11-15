package io.github.inarazahin.employees.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Employees API")
class EmployeeApiTest extends ApiTestBase {

    private static String randomLetters(int length) {
        var random = java.util.concurrent.ThreadLocalRandom.current();
        StringBuilder letters = new StringBuilder();
        for (int i = 0; i < length; i++) {
            letters.append((char) ('a' + random.nextInt(26)));
        }
        return letters.toString();
    }

    @Nested
    @DisplayName("create and read")
    class CreateAndRead {

        @Test
        @DisplayName("POST returns 201, a Location header and the stored employee")
        void create() {
            long department = createDepartment();
            Map<String, Object> body = employee(department);

            String location = asAdmin().body(body)
                    .post("/api/v1/employees")
                    .then().statusCode(201)
                    .header("Location", containsString("/api/v1/employees/"))
                    .body("firstName", equalTo("Ada"))
                    .body("email", equalTo(body.get("email")))
                    .body("salary", equalTo(75000.0f))
                    .body("department.id", equalTo((int) department))
                    .extract().header("Location");

            asViewer().get(location)
                    .then().statusCode(200)
                    .body("email", equalTo(body.get("email")))
                    .body("hireDate", equalTo(body.get("hireDate")));
        }

        @Test
        @DisplayName("email is stored normalised to lower case")
        void emailIsNormalised() {
            Map<String, Object> body = employee(createDepartment());
            body.put("email", "Mixed.Case." + unique("x") + "@Example.COM");

            asAdmin().body(body).post("/api/v1/employees")
                    .then().statusCode(201)
                    .body("email", equalTo(((String) body.get("email")).toLowerCase()));
        }

        @Test
        @DisplayName("an unknown id is 404 with a problem document")
        void unknownEmployee() {
            asViewer().get("/api/v1/employees/999999999")
                    .then().statusCode(404)
                    .contentType("application/problem+json")
                    .body("detail", equalTo("Employee 999999999 not found"));
        }
    }

    @Nested
    @DisplayName("list, filter and search")
    class Listing {

        @Test
        @DisplayName("filters by department and pages and sorts the result")
        void filterPageAndSort() {
            long department = createDepartment();
            for (String first : List.of("Cleo", "Abel", "Bea")) {
                Map<String, Object> body = employee(department);
                body.put("firstName", first);
                createEmployee(body);
            }

            asViewer().queryParam("departmentId", department)
                    .queryParam("size", 2)
                    .queryParam("sort", "firstName,asc")
                    .get("/api/v1/employees")
                    .then().statusCode(200)
                    .body("page.totalElements", equalTo(3))
                    .body("page.totalPages", equalTo(2))
                    .body("content.firstName", contains("Abel", "Bea"));
        }

        @Test
        @DisplayName("search matches name and email, case-insensitively")
        void search() {
            long department = createDepartment();
            String surname = "Zx" + randomLetters(8);
            Map<String, Object> body = employee(department);
            body.put("lastName", surname);
            createEmployee(body);

            asViewer().queryParam("search", surname.toUpperCase())
                    .get("/api/v1/employees")
                    .then().statusCode(200)
                    .body("content.lastName", everyItem(equalTo(surname)))
                    .body("page.totalElements", equalTo(1));

            String emailFragment = ((String) body.get("email")).substring(4, 12);
            asViewer().queryParam("search", emailFragment)
                    .get("/api/v1/employees")
                    .then().statusCode(200)
                    .body("content.email", hasItem(body.get("email")));
        }
    }

    @Nested
    @DisplayName("update and delete")
    class UpdateAndDelete {

        @Test
        @DisplayName("PUT replaces the employee's details, including department")
        void update() {
            long from = createDepartment();
            long to = createDepartment();
            Map<String, Object> body = employee(from);
            long id = createEmployee(body);

            body.put("jobTitle", "Senior Analyst");
            body.put("salary", 88000.50);
            body.put("departmentId", to);

            asAdmin().body(body).put("/api/v1/employees/" + id)
                    .then().statusCode(200)
                    .body("jobTitle", equalTo("Senior Analyst"))
                    .body("salary", equalTo(88000.5f))
                    .body("department.id", equalTo((int) to));
        }

        @Test
        @DisplayName("DELETE removes the employee; a second read is 404")
        void delete() {
            long id = createEmployee(employee(createDepartment()));

            asAdmin().delete("/api/v1/employees/" + id).then().statusCode(204);
            asAdmin().get("/api/v1/employees/" + id).then().statusCode(404);
            asAdmin().delete("/api/v1/employees/" + id).then().statusCode(404);
        }
    }

    @Nested
    @DisplayName("business rules and validation")
    class Rules {

        @Test
        @DisplayName("email addresses are unique, ignoring case")
        void duplicateEmail() {
            long department = createDepartment();
            Map<String, Object> first = employee(department);
            createEmployee(first);

            Map<String, Object> second = employee(department);
            second.put("email", ((String) first.get("email")).toUpperCase());

            asAdmin().body(second).post("/api/v1/employees")
                    .then().statusCode(409)
                    .body("detail", containsString("already exists"));
        }

        @Test
        @DisplayName("updating an employee may keep its own email")
        void updateKeepsOwnEmail() {
            Map<String, Object> body = employee(createDepartment());
            long id = createEmployee(body);
            body.put("jobTitle", "Lead");

            asAdmin().body(body).put("/api/v1/employees/" + id).then().statusCode(200);
        }

        @Test
        @DisplayName("a missing department is 422 Unprocessable Content")
        void unknownDepartment() {
            asAdmin().body(employee(999_999_999L)).post("/api/v1/employees")
                    .then().statusCode(422)
                    .body("detail", equalTo("Department 999999999 does not exist"));
        }

        @Test
        @DisplayName("every invalid field is reported at once")
        void reportsAllInvalidFields() {
            asAdmin().body(Map.of("firstName", "", "email", "not-an-email", "salary", -5))
                    .post("/api/v1/employees")
                    .then().statusCode(400)
                    .body("detail", equalTo("Validation failed"))
                    .body("errors", hasKey("firstName"))
                    .body("errors", hasKey("lastName"))
                    .body("errors", hasKey("email"))
                    .body("errors", hasKey("jobTitle"))
                    .body("errors", hasKey("salary"))
                    .body("errors", hasKey("hireDate"))
                    .body("errors", hasKey("departmentId"));
        }

        static List<Arguments> invalidValues() {
            return List.of(
                    Arguments.of("firstName", "R2D2", "must contain only letters"),
                    Arguments.of("email", "missing-at.example.com", "must be a well-formed email address"),
                    Arguments.of("salary", 0, "must be greater than 0.00"),
                    Arguments.of("salary", 12.345, "numeric value out of bounds"),
                    Arguments.of("hireDate", LocalDate.now().plusDays(1).toString(),
                            "must be a date in the past or in the present"));
        }

        @ParameterizedTest(name = "{0} = {1} is rejected")
        @MethodSource("invalidValues")
        @DisplayName("field rules")
        void fieldRules(String field, Object value, String message) {
            Map<String, Object> body = employee(createDepartment());
            body.put(field, value);

            asAdmin().body(body).post("/api/v1/employees")
                    .then().statusCode(400)
                    .body("errors." + field, containsString(message))
                    .body("errors", not(hasKey("departmentId")));
        }

        @Test
        @DisplayName("malformed JSON is a 400, not a server error")
        void malformedJson() {
            asAdmin().body("{\"firstName\": ").post("/api/v1/employees")
                    .then().statusCode(400);
        }
    }
}
