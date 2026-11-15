package io.github.inarazahin.employees.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.inarazahin.employees.department.Department;
import io.github.inarazahin.employees.department.DepartmentRepository;
import io.github.inarazahin.employees.employee.Employee;
import io.github.inarazahin.employees.employee.EmployeeDtos.EmployeeRequest;
import io.github.inarazahin.employees.employee.EmployeeRepository;
import io.github.inarazahin.employees.employee.EmployeeService;
import io.github.inarazahin.employees.error.ApiException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** Business rules in isolation: no Spring context, no database. */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService")
class EmployeeServiceTest {

    @Mock
    EmployeeRepository employees;

    @Mock
    DepartmentRepository departments;

    @InjectMocks
    EmployeeService service;

    private static EmployeeRequest request(String email, long departmentId) {
        return new EmployeeRequest(" Grace ", "Hopper", email, "Engineer", new BigDecimal("100000.00"),
                LocalDate.of(2020, 1, 1), departmentId);
    }

    @Test
    @DisplayName("creates an employee with trimmed names and a lower-cased email")
    void createsNormalisedEmployee() {
        Department department = new Department("Engineering", null);
        when(departments.findById(1L)).thenReturn(Optional.of(department));
        when(employees.save(any(Employee.class))).thenAnswer(call -> call.getArgument(0));

        var created = service.create(request(" Grace.Hopper@Example.com ", 1L));

        assertThat(created.firstName()).isEqualTo("Grace");
        assertThat(created.email()).isEqualTo("grace.hopper@example.com");
        assertThat(created.department().name()).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("rejects a duplicate email with 409 and saves nothing")
    void rejectsDuplicateEmail() {
        when(employees.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("taken@example.com", 1L)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT));
        verify(employees, never()).save(any());
    }

    @Test
    @DisplayName("rejects an unknown department with 422")
    void rejectsUnknownDepartment() {
        when(departments.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request("new@example.com", 42L)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT))
                .hasMessage("Department 42 does not exist");
    }

    @Test
    @DisplayName("an update may keep the employee's own email")
    void updateKeepsOwnEmail() {
        Department department = new Department("Engineering", null);
        Employee existing = new Employee("Grace", "Hopper", "grace@example.com", "Engineer",
                new BigDecimal("1.00"), LocalDate.of(2020, 1, 1), department);
        when(employees.findById(7L)).thenReturn(Optional.of(existing));
        when(employees.existsByEmailIgnoreCaseAndIdNot("grace@example.com", 7L)).thenReturn(false);
        when(departments.findById(1L)).thenReturn(Optional.of(department));

        var updated = service.update(7L, request("grace@example.com", 1L));

        assertThat(updated.salary()).isEqualByComparingTo("100000.00");
    }

    @Test
    @DisplayName("deleting an unknown employee is 404")
    void deleteUnknown() {
        when(employees.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
