CREATE TABLE buildings (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    address VARCHAR(200) NOT NULL,
    district VARCHAR(80),
    city VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE apartments (
    id BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES buildings(id),
    number VARCHAR(20) NOT NULL,
    floor INTEGER NOT NULL,
    area_m2 NUMERIC(8, 2),
    occupied BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (building_id, number)
);

CREATE TABLE residents (
    id BIGSERIAL PRIMARY KEY,
    apartment_id BIGINT NOT NULL REFERENCES apartments(id),
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    document_number VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(120),
    phone VARCHAR(40),
    owner BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    apartment_id BIGINT NOT NULL REFERENCES apartments(id),
    concept VARCHAR(120) NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    due_date DATE NOT NULL,
    paid_at DATE,
    status VARCHAR(20) NOT NULL
);
