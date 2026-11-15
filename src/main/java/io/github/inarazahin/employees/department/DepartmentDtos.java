package io.github.inarazahin.employees.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request and response bodies for the department endpoints. */
public final class DepartmentDtos {

    private DepartmentDtos() {
    }

    public record DepartmentRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String description) {
    }

    public record DepartmentResponse(Long id, String name, String description, long employeeCount) {
    }
}
