package io.github.inarazahin.employees.employee;

import io.github.inarazahin.employees.department.Department;
import io.github.inarazahin.employees.department.DepartmentRepository;
import io.github.inarazahin.employees.employee.EmployeeDtos.EmployeeRequest;
import io.github.inarazahin.employees.employee.EmployeeDtos.EmployeeResponse;
import io.github.inarazahin.employees.error.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employees;
    private final DepartmentRepository departments;

    public EmployeeService(EmployeeRepository employees, DepartmentRepository departments) {
        this.employees = employees;
        this.departments = departments;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> search(Long departmentId, String search, Pageable pageable) {
        String term = search == null || search.isBlank() ? null : search.trim();
        return employees.search(departmentId, term, pageable).map(EmployeeResponse::from);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long id) {
        return EmployeeResponse.from(get(id));
    }

    public EmployeeResponse create(EmployeeRequest request) {
        String email = normalise(request.email());
        if (employees.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An employee with email " + email + " already exists");
        }
        Employee employee = new Employee(request.firstName().trim(), request.lastName().trim(), email,
                request.jobTitle().trim(), request.salary(), request.hireDate(), department(request.departmentId()));
        return EmployeeResponse.from(employees.save(employee));
    }

    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = get(id);
        String email = normalise(request.email());
        if (employees.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw ApiException.conflict("An employee with email " + email + " already exists");
        }
        employee.update(request.firstName().trim(), request.lastName().trim(), email, request.jobTitle().trim(),
                request.salary(), request.hireDate(), department(request.departmentId()));
        return EmployeeResponse.from(employee);
    }

    public void delete(Long id) {
        employees.delete(get(id));
    }

    private Employee get(Long id) {
        return employees.findById(id).orElseThrow(() -> ApiException.notFound("Employee", id));
    }

    /** A reference to a missing department is a semantic error in the request body: 422, not 404. */
    private Department department(Long departmentId) {
        return departments.findById(departmentId)
                .orElseThrow(() -> ApiException.unprocessable("Department " + departmentId + " does not exist"));
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase();
    }
}
