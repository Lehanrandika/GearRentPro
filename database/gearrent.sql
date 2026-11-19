-- Import once into a fresh database. Keep the original gearrent_pro practice database.
CREATE DATABASE IF NOT EXISTS gearrent_coursework CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gearrent_coursework;
CREATE TABLE branch (
 branch_code VARCHAR(10) PRIMARY KEY, name VARCHAR(100) NOT NULL, address VARCHAR(255) NOT NULL, contact VARCHAR(20) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE category (
 category_code VARCHAR(10) PRIMARY KEY, name VARCHAR(50) NOT NULL, description VARCHAR(255) NOT NULL,
 base_price_factor DECIMAL(8,2) NOT NULL CHECK(base_price_factor>0),
 weekend_multiplier DECIMAL(8,2) NOT NULL CHECK(weekend_multiplier>0), is_active BOOLEAN NOT NULL DEFAULT TRUE,
 branch_code VARCHAR(10), FOREIGN KEY(branch_code) REFERENCES branch(branch_code)
) ENGINE=InnoDB;
CREATE TABLE membership_level (
 level_name VARCHAR(20) PRIMARY KEY, discount_percent DECIMAL(5,2) NOT NULL CHECK(discount_percent BETWEEN 0 AND 100)
) ENGINE=InnoDB;
CREATE TABLE customer (
 customer_id VARCHAR(10) PRIMARY KEY, name VARCHAR(100) NOT NULL, nic_or_passport VARCHAR(30) NOT NULL UNIQUE,
 contact_no VARCHAR(20) NOT NULL, email VARCHAR(150) NOT NULL, address VARCHAR(255) NOT NULL, level_name VARCHAR(20),
 FOREIGN KEY(level_name) REFERENCES membership_level(level_name)
) ENGINE=InnoDB;
CREATE TABLE equipment (
 equipment_id VARCHAR(10) PRIMARY KEY, brand VARCHAR(50) NOT NULL, model VARCHAR(100) NOT NULL, purchase_year YEAR NOT NULL,
 daily_base_price DECIMAL(12,2) NOT NULL CHECK(daily_base_price>=0), security_deposit DECIMAL(12,2) NOT NULL CHECK(security_deposit>=0),
 status ENUM('Available','Reserved','Rented','Under Maintenance') NOT NULL DEFAULT 'Available',
 branch_code VARCHAR(10) NOT NULL, category_code VARCHAR(10) NOT NULL,
 FOREIGN KEY(branch_code) REFERENCES branch(branch_code), FOREIGN KEY(category_code) REFERENCES category(category_code)
) ENGINE=InnoDB;
CREATE TABLE app_user (
 username VARCHAR(50) PRIMARY KEY, salt_hex CHAR(32) NOT NULL, hash_hex CHAR(64) NOT NULL,
 role ENUM('Admin','Branch Manager','Staff') NOT NULL, branch_code VARCHAR(10), can_edit_equipment BOOLEAN NOT NULL DEFAULT FALSE,
 FOREIGN KEY(branch_code) REFERENCES branch(branch_code)
) ENGINE=InnoDB;
CREATE TABLE settings (
 id INT PRIMARY KEY, deposit_limit DECIMAL(14,2) NOT NULL, long_discount DECIMAL(5,2) NOT NULL,
 late_fee DECIMAL(10,2) NOT NULL, CHECK(deposit_limit>=0 AND long_discount BETWEEN 0 AND 100 AND late_fee>=0)
) ENGINE=InnoDB;
CREATE TABLE reservation (
 reservation_id INT PRIMARY KEY AUTO_INCREMENT, equipment_id VARCHAR(10) NOT NULL, customer_id VARCHAR(10) NOT NULL,
 branch_code VARCHAR(10) NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL,
 status ENUM('Reserved','Converted','Cancelled') NOT NULL DEFAULT 'Reserved',
 FOREIGN KEY(equipment_id) REFERENCES equipment(equipment_id), FOREIGN KEY(customer_id) REFERENCES customer(customer_id),
 FOREIGN KEY(branch_code) REFERENCES branch(branch_code), INDEX(equipment_id,status,start_date,end_date)
) ENGINE=InnoDB;
CREATE TABLE rental (
 rental_id INT PRIMARY KEY AUTO_INCREMENT, reservation_id INT UNIQUE, equipment_id VARCHAR(10) NOT NULL, customer_id VARCHAR(10) NOT NULL,
 branch_code VARCHAR(10) NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL, gross DECIMAL(16,2) NOT NULL,
 long_discount DECIMAL(16,2) NOT NULL, member_discount DECIMAL(16,2) NOT NULL, net_rent DECIMAL(16,2) NOT NULL,
 deposit DECIMAL(12,2) NOT NULL, payable DECIMAL(16,2) NOT NULL, payment ENUM('Paid','Partially Paid','Unpaid') NOT NULL,
 status ENUM('Active','Returned','Cancelled') NOT NULL DEFAULT 'Active', returned_on DATE, damage_description VARCHAR(255),
 late_charge DECIMAL(16,2) NOT NULL DEFAULT 0, damage_charge DECIMAL(16,2) NOT NULL DEFAULT 0,
 refund DECIMAL(16,2) NOT NULL DEFAULT 0, extra_due DECIMAL(16,2) NOT NULL DEFAULT 0,
 FOREIGN KEY(reservation_id) REFERENCES reservation(reservation_id), FOREIGN KEY(equipment_id) REFERENCES equipment(equipment_id),
 FOREIGN KEY(customer_id) REFERENCES customer(customer_id), FOREIGN KEY(branch_code) REFERENCES branch(branch_code),
 INDEX(equipment_id,status,start_date,end_date)
) ENGINE=InnoDB;
CREATE TABLE maintenance (
 maintenance_id INT PRIMARY KEY AUTO_INCREMENT, equipment_id VARCHAR(10) NOT NULL, start_date DATE NOT NULL, end_exclusive DATE,
 FOREIGN KEY(equipment_id) REFERENCES equipment(equipment_id)
) ENGINE=InnoDB;
CREATE VIEW rental_list AS SELECT r.*, CASE WHEN status='Active' AND end_date<CURRENT_DATE THEN 'Overdue' ELSE status END AS rental_status FROM rental r;
CREATE VIEW inventory AS SELECT e.equipment_id,e.brand,e.model,e.purchase_year,e.daily_base_price,e.security_deposit,
 CASE WHEN e.status='Under Maintenance' THEN 'Under Maintenance'
 WHEN EXISTS(SELECT 1 FROM rental r WHERE r.equipment_id=e.equipment_id AND r.status='Active' AND r.start_date<=CURRENT_DATE) THEN 'Rented'
 WHEN EXISTS(SELECT 1 FROM reservation b WHERE b.equipment_id=e.equipment_id AND b.status='Reserved' AND CURRENT_DATE BETWEEN b.start_date AND b.end_date) THEN 'Reserved'
 ELSE 'Available' END AS status,e.branch_code,e.category_code FROM equipment e;
START TRANSACTION;
INSERT INTO branch VALUES ('BR001','Galle Branch','Main Street, Galle','0912223344'),('BR002','Panadura Branch','Station Road, Panadura','0382223344'),('BR003','Colombo Branch','Park Road, Colombo','0112223344');
INSERT INTO membership_level VALUES ('Regular',0),('Silver',5),('Gold',10);
INSERT INTO settings VALUES (1,500000,8,500);
INSERT INTO category VALUES
 ('CAT01','Camera','Digital cameras',1,1.2,1,NULL),('CAT02','Lens','Camera lenses',1,1.1,1,NULL),
 ('CAT03','Drone','Camera drones',1.5,1.2,1,NULL),('CAT04','Lighting','Studio lighting',1,1.1,1,NULL),
 ('CAT05','Audio','Microphones and recorders',1,1,1,NULL);
INSERT INTO customer VALUES
 ('CU001','Nimal Perera','TEST-P001','0770000001','nimal@example.com','Galle','Silver'),
 ('CU002','Amali Silva','TEST-P002','0770000002','amali@example.com','Panadura','Regular'),
 ('CU003','Ravi Fernando','TEST-P003','0770000003','ravi@example.com','Colombo','Gold'),
 ('CU004','Simali Dias','TEST-P004','0770000004','simali@example.com','Galle',NULL),
 ('CU005','Kamal De Silva','TEST-P005','0770000005','kamal@example.com','Panadura','Silver'),
 ('CU006','Anu Jayasuriya','TEST-P006','0770000006','anu@example.com','Colombo','Regular'),
 ('CU007','Dilan Costa','TEST-P007','0770000007','dilan@example.com','Galle','Gold'),
 ('CU008','Manel De Mel','TEST-P008','0770000008','manel@example.com','Panadura',NULL),
 ('CU009','Isuru Peris','TEST-P009','0770000009','isuru@example.com','Colombo','Silver'),
 ('CU010','Nadee Kumar','TEST-P010','0770000010','nadee@example.com','Galle','Regular');
INSERT INTO equipment VALUES
 ('EQ001','Canon','EOS 200D',2024,2000,10000,'Available','BR001','CAT01'),
 ('EQ002','Sigma','35mm Art',2024,1000,5000,'Available','BR002','CAT02'),
 ('EQ003','DJI','Mini 3',2024,3000,20000,'Available','BR003','CAT03'),
 ('EQ004','Godox','SL60W',2024,1500,7500,'Available','BR001','CAT04'),
 ('EQ005','Zoom','H1n',2024,800,4000,'Available','BR002','CAT05'),
 ('EQ006','Sony','Alpha A6400',2024,2000,10000,'Available','BR003','CAT01'),
 ('EQ007','Nikon','50mm',2024,1000,5000,'Available','BR001','CAT02'),
 ('EQ008','DJI','Air 2S',2024,3000,20000,'Available','BR002','CAT03'),
 ('EQ009','Aputure','120D',2024,1500,7500,'Available','BR003','CAT04'),
 ('EQ010','Rode','VideoMic',2024,800,4000,'Under Maintenance','BR001','CAT05'),
 ('EQ011','Canon','EOS 200D',2024,2000,10000,'Available','BR002','CAT01'),
 ('EQ012','Sigma','35mm Art',2024,1000,5000,'Available','BR003','CAT02'),
 ('EQ013','DJI','Mini 3',2024,3000,20000,'Available','BR001','CAT03'),
 ('EQ014','Godox','SL60W',2024,1500,7500,'Available','BR002','CAT04'),
 ('EQ015','Zoom','H1n',2024,800,4000,'Available','BR003','CAT05'),
 ('EQ016','Sony','Alpha A6400',2024,2000,10000,'Available','BR001','CAT01'),
 ('EQ017','Nikon','50mm',2024,1000,5000,'Available','BR002','CAT02'),
 ('EQ018','DJI','Air 2S',2024,3000,20000,'Available','BR003','CAT03'),
 ('EQ019','Aputure','120D',2024,1500,7500,'Available','BR001','CAT04'),
 ('EQ020','Rode','VideoMic',2024,800,4000,'Available','BR002','CAT05'),
 ('EQ021','Canon','EOS 200D',2024,2000,10000,'Available','BR003','CAT01');
INSERT INTO app_user VALUES
 ('admin','51dec913babc3a1054a0e2c925caa2ba','d2f72be083fd6dbf55b3504ffcc94c6472938cea7d36378d784eae61ec46aee9','Admin',NULL,1),
 ('manager','426d530cee405529bab2180c07992fd2','6e68d9b500e16a3423fca044f26666be7e2c33c6085548aa71e651e85c61aba0','Branch Manager','BR001',1),
 ('staff','7a4faa5caaa84c90b76ef9b318d9bde2','336342a2966c098cd6a69bcc1b9ebfe28207f10a16528828d9ee097d345e3eed','Staff','BR001',1);
-- Audio has a 1.00 weekend multiplier; CU002 is Regular.
INSERT INTO rental(equipment_id,customer_id,branch_code,start_date,end_date,gross,long_discount,member_discount,net_rent,deposit,payable,payment,status)
VALUES ('EQ005','CU002','BR002',CURRENT_DATE-INTERVAL 5 DAY,CURRENT_DATE-INTERVAL 3 DAY,2400,0,0,2400,4000,6400,'Paid','Active');
INSERT INTO rental(equipment_id,customer_id,branch_code,start_date,end_date,gross,long_discount,member_discount,net_rent,deposit,payable,payment,status,returned_on,damage_description,late_charge,damage_charge,refund)
VALUES ('EQ010','CU002','BR001',CURRENT_DATE-INTERVAL 6 DAY,CURRENT_DATE-INTERVAL 5 DAY,1600,0,0,1600,4000,5600,'Paid','Returned',CURRENT_DATE-INTERVAL 4 DAY,'Cracked microphone mount',500,750,2750);
INSERT INTO maintenance(equipment_id,start_date) VALUES ('EQ010',CURRENT_DATE-INTERVAL 3 DAY);
INSERT INTO reservation(equipment_id,customer_id,branch_code,start_date,end_date)
VALUES ('EQ001','CU001','BR001',CURRENT_DATE+INTERVAL 2 DAY,CURRENT_DATE+INTERVAL 4 DAY);
COMMIT;
