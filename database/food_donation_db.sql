-- =====================================================
-- Food Donation Record Management System - MySQL Database
-- Student: Ms. Jiya Agnihotri | Reg No: PIET24CR035
-- Run in MySQL Workbench (root@localhost:3306): paste, then press the lightning icon
-- =====================================================

DROP DATABASE IF EXISTS food_donation_db;
CREATE DATABASE food_donation_db;
USE food_donation_db;

-- Donor details
CREATE TABLE donors (
    donor_id    INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    donor_type  VARCHAR(40)  NOT NULL DEFAULT 'Individual',
    phone       VARCHAR(15)  NOT NULL,
    email       VARCHAR(100),
    address     VARCHAR(200),
    registered_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Food donations received (remaining_quantity reduces as food is distributed)
CREATE TABLE donations (
    donation_id        INT AUTO_INCREMENT PRIMARY KEY,
    donor_id           INT NOT NULL,
    food_type          VARCHAR(60) NOT NULL,
    quantity           DECIMAL(10,2) NOT NULL CHECK (quantity > 0),
    unit               VARCHAR(20) NOT NULL DEFAULT 'kg',
    remaining_quantity DECIMAL(10,2) NOT NULL CHECK (remaining_quantity >= 0),
    donation_date      DATE NOT NULL,
    expiry_date        DATE NULL,
    FOREIGN KEY (donor_id) REFERENCES donors(donor_id)
);

-- Distribution of donated food to beneficiaries
CREATE TABLE distributions (
    dist_id      INT AUTO_INCREMENT PRIMARY KEY,
    donation_id  INT NOT NULL,
    beneficiary  VARCHAR(120) NOT NULL,
    location     VARCHAR(120),
    quantity     DECIMAL(10,2) NOT NULL CHECK (quantity > 0),
    dist_date    DATE NOT NULL,
    FOREIGN KEY (donation_id) REFERENCES donations(donation_id)
);

-- ---------------- Sample data ----------------
INSERT INTO donors (name, donor_type, phone, email, address) VALUES
('Hotel Rajmahal',        'Hotel',           '9414000001', 'rajmahal@example.com',  'MI Road, Jaipur'),
('Sharma Catering',       'Wedding / Event', '9414000002', 'sharma.cat@example.com','Malviya Nagar, Jaipur'),
('Anita Verma',           'Individual',      '9414000003', 'anita.v@example.com',   'Vaishali Nagar, Jaipur'),
('Green Leaf Restaurant', 'Restaurant',      '9414000004', 'greenleaf@example.com', 'C-Scheme, Jaipur'),
('Annapurna Foundation',  'Organization',    '9414000005', 'annapurna@example.com', 'Mansarovar, Jaipur'),
('Ramesh Gupta',          'Individual',      '9414000006', NULL,                    'Jagatpura, Jaipur');

INSERT INTO donations (donor_id, food_type, quantity, unit, remaining_quantity, donation_date, expiry_date) VALUES
(1, 'Cooked Meals',          50.00,  'plates',  20.00, '2026-11-10', '2026-11-11'),
(2, 'Grains / Rice / Wheat', 100.00, 'kg',      60.00, '2026-11-11', NULL),
(3, 'Vegetables',            25.00,  'kg',       0.00, '2026-11-12', '2026-11-15'),
(4, 'Bakery Items',          40.00,  'packets', 40.00, '2026-11-14', '2026-11-18'),
(5, 'Fruits',                30.00,  'kg',      20.00, '2026-11-15', '2026-11-19'),
(1, 'Packaged Food',         60.00,  'packets', 60.00, '2026-11-17', '2027-03-01');

INSERT INTO distributions (donation_id, beneficiary, location, quantity, dist_date) VALUES
(1, 'Asha Shelter Home',          'Jaipur',          30.00, '2026-11-10'),
(2, 'Seva Sansthan Orphanage',    'Sanganer',        40.00, '2026-11-12'),
(3, 'Railway Station Feeding Drive','Jaipur Junction',25.00, '2026-11-12'),
(5, 'Vridh Ashram (Old Age Home)','Mansarovar',      10.00, '2026-11-16');

-- Check data
SELECT * FROM donors;
SELECT * FROM donations;
SELECT * FROM distributions;

-- Useful queries (for demo / viva)
-- Food-wise report:
-- SELECT food_type, unit, SUM(quantity) AS donated, SUM(quantity-remaining_quantity) AS distributed,
--        SUM(remaining_quantity) AS remaining FROM donations GROUP BY food_type, unit;
-- Donor-wise donations:
-- SELECT o.name, COUNT(*) AS total_donations FROM donors o JOIN donations d ON o.donor_id=d.donor_id GROUP BY o.name;
