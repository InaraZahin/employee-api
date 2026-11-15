package io.github.inarazahin.employees.employee;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Request and response bodies for the employee endpoints. */
public final class EmployeeDtos {

    private EmployeeDtos() {
    }

    private static final String NAME = "^[\\p{L}][\\p{L} .'-]*$";

    public record EmployeeRequest(
            @NotBlank @Size(max = 60) @Pattern(regexp = NAME, message = "must contain only letters") String firstName,
            @NotBlank @Size(max = 60) @Pattern(regexp = NAME, message = "must contain only letters") String lastName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 100) String jobTitle,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2)
            BigDecimal salary,
            @NotNull @PastOrPresent LocalDate hireDate,
            @NotNull Long departmentId) {
    }

    public record DepartmentRef(Long id, String name) {
    }

    public record EmployeeResponse(Long id, String firstName, String lastName, String email, String jobTitle,
            BigDecimal salary, LocalDate hireDate, DepartmentRef department) {

        static EmployeeResponse from(Employee e) {
            return new EmployeeResponse(e.getId(), e.getFirstName(), e.getLastName(), e.getEmail(), e.getJobTitle(),
                    e.getSalary(), e.getHireDate(),
                    new DepartmentRef(e.getDepartment().getId(), e.getDepartment().getName()));
        }
    }
}
