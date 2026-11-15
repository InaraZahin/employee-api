package io.github.inarazahin.employees.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByDepartmentId(Long departmentId);

    /** Optional filters: department and a case-insensitive search over name and email. */
    @EntityGraph(attributePaths = "department")
    @Query("""
            select e from Employee e
            where (:departmentId is null or e.department.id = :departmentId)
              and (:search is null
                   or lower(e.firstName) like lower(concat('%', cast(:search as string), '%'))
                   or lower(e.lastName) like lower(concat('%', cast(:search as string), '%'))
                   or lower(e.email) like lower(concat('%', cast(:search as string), '%')))
            """)
    Page<Employee> search(@Param("departmentId") Long departmentId, @Param("search") String search,
            Pageable pageable);
}
