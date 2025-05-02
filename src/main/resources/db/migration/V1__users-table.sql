create table users (
    id serial primary key,
    cpf varchar(14) unique,
    email text unique,
    password text,
    name text,
    birthdate date,
    gender text,
    cep bigint,
    address text
);