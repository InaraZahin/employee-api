package io.github.inarazahin.employees.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Departments API")
class DepartmentApiTest extends ApiTestBase {

    @Test
    @DisplayName("create, read, rename and delete a department")
    void lifecycle() {
        String name = unique("Research");
        long id = createDepartment(name);

        asViewer().get("/api/v1/departments/" + id)
                .then().statusCode(200)
                .body("name", equalTo(name))
                .body("employeeCount", equalTo(0));
        asViewer().get("/api/v1/departments")
                .then().statusCode(200)
                .body("name", hasItem(name));

        String renamed = unique("Labs");
        asAdmin().body(Map.of("name", renamed)).put("/api/v1/departments/" + id)
                .then().statusCode(200)
                .body("name", equalTo(renamed));

        asAdmin().delete("/api/v1/departments/" + id).then().statusCode(204);
        asViewer().get("/api/v1/departments/" + id).then().statusCode(404);
    }

    @Test
    @DisplayName("names are unique, ignoring case")
    void duplicateName() {
        String name = unique("Finance");
        createDepartment(name);

        asAdmin().body(Map.of("name", name.toUpperCase())).post("/api/v1/departments")
                .then().statusCode(409)
                .body("detail", equalTo("Department '" + name.toUpperCase() + "' already exists"));
    }

    @Test
    @DisplayName("a department with employees cannot be deleted, and reports its head count")
    void cannotDeleteNonEmpty() {
        long id = createDepartment();
        createEmployee(employee(id));
        createEmployee(employee(id));

        asViewer().get("/api/v1/departments/" + id).then().body("employeeCount", equalTo(2));
        asAdmin().delete("/api/v1/departments/" + id)
                .then().statusCode(409)
                .body("detail", equalTo("Department " + id + " still has 2 employee(s)"));
    }

    @Test
    @DisplayName("seeded demo data is available after migration")
    void demoData() {
        asViewer().get("/api/v1/departments")
                .then().statusCode(200)
                .body("name", hasItem("Quality Assurance"));
    }
}
