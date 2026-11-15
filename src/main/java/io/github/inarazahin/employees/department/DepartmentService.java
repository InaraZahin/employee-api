package io.github.inarazahin.employees.department;

import io.github.inarazahin.employees.department.DepartmentDtos.DepartmentRequest;
import io.github.inarazahin.employees.department.DepartmentDtos.DepartmentResponse;
import io.github.inarazahin.employees.employee.EmployeeRepository;
import io.github.inarazahin.employees.error.ApiException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departments;
    private final EmployeeRepository employees;

    public DepartmentService(DepartmentRepository departments, EmployeeRepository employees) {
        this.departments = departments;
        this.employees = employees;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> findAll() {
        return departments.findAll(Sort.by("name")).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse findById(Long id) {
        return toResponse(get(id));
    }

    public DepartmentResponse create(DepartmentRequest request) {
        requireUniqueName(request.name(), null);
        return toResponse(departments.save(new Department(request.name().trim(), request.description())));
    }

    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = get(id);
        requireUniqueName(request.name(), id);
        department.update(request.name().trim(), request.description());
        return toResponse(department);
    }

    public void delete(Long id) {
        Department department = get(id);
        long staff = employees.countByDepartmentId(id);
        if (staff > 0) {
            throw ApiException.conflict("Department " + id + " still has " + staff + " employee(s)");
        }
        departments.delete(department);
    }

    public Department get(Long id) {
        return departments.findById(id).orElseThrow(() -> ApiException.notFound("Department", id));
    }

    private void requireUniqueName(String name, Long ownId) {
        departments.findByNameIgnoreCase(name.trim())
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw ApiException.conflict("Department '" + name.trim() + "' already exists");
                });
    }

    private DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(d.getId(), d.getName(), d.getDescription(),
                employees.countByDepartmentId(d.getId()));
    }
}
