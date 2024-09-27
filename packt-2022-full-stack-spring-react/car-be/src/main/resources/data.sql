delete from spring.authorities;
delete from spring.users;
insert into spring.users (username, password, enabled) values ('john', '12345', true);
insert into spring.authorities (username, authority) values ('john', 'write');

delete from car.car;
delete from car.owner;
insert into car.owner (id, first_name, last_name) values (1, 'John', 'Doe');
insert into car.owner (id, first_name, last_name) values (2, 'Mary', 'Jane');
insert into car.car (id, brand, model, color, register_number, manufacturing_year, price, owner_id) values (11, 'Toyota', 'Camry', 'Black', 'DL-001', 2020, 35000, 1);
insert into car.car (id, brand, model, color, register_number, manufacturing_year, price, owner_id) values (12, 'Honda', 'Civic', 'White', 'DL-002', 2021, 30000, 1);
insert into car.car (id, brand, model, color, register_number, manufacturing_year, price, owner_id) values (21, 'Ford', 'Fusion', 'Silver', 'DL-003', 2019, 32000, 2);
insert into car.car (id, brand, model, color, register_number, manufacturing_year, price, owner_id) values (22, 'Chevrolet', 'Malibu', 'Red', 'DL-004', 2020, 33000, 2);
insert into car.car (id, brand, model, color, register_number, manufacturing_year, price, owner_id) values (23, 'Nissan', 'Altima', 'Blue', 'DL-005', 2021, 31000, 2);
