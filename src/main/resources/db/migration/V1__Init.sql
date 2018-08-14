create table users (
  id uuid primary key,
  login varchar(255) not null,
  password varchar(255) not null,
  email varchar(255)
);