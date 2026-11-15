CREATE TABLE departments (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255)
);
CREATE UNIQUE INDEX departments_name_uq ON departments (LOWER(name));

CREATE TABLE employees (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name    VARCHAR(60)    NOT NULL,
    last_name     VARCHAR(60)    NOT NULL,
    email         VARCHAR(254)   NOT NULL,
    job_title     VARCHAR(100)   NOT NULL,
    salary        NUMERIC(12, 2) NOT NULL CHECK (salary > 0),
    hire_date     DATE           NOT NULL,
    department_id BIGINT         NOT NULL REFERENCES departments (id)
);
CREATE UNIQUE INDEX employees_email_uq ON employees (LOWER(email));
CREATE INDEX employees_department_idx ON employees (department_id);

CREATE TABLE app_users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('VIEWER', 'ADMIN'))
);
