INSERT INTO departments (name, description) VALUES
    ('Engineering', 'Builds and runs the product'),
    ('Quality Assurance', 'Keeps the product working'),
    ('People', 'Hiring and employee experience');

INSERT INTO employees (first_name, last_name, email, job_title, salary, hire_date, department_id) VALUES
    ('Maya', 'Chen', 'maya.chen@example.com', 'Backend Engineer', 98000.00, '2022-03-14',
        (SELECT id FROM departments WHERE name = 'Engineering')),
    ('Omar', 'Haddad', 'omar.haddad@example.com', 'Frontend Engineer', 91000.00, '2023-07-01',
        (SELECT id FROM departments WHERE name = 'Engineering')),
    ('Lena', 'Novak', 'lena.novak@example.com', 'QA Engineer', 82000.00, '2021-11-22',
        (SELECT id FROM departments WHERE name = 'Quality Assurance')),
    ('Sam', 'Okafor', 'sam.okafor@example.com', 'Recruiter', 64000.00, '2024-01-08',
        (SELECT id FROM departments WHERE name = 'People'));
