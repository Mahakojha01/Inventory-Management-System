CREATE DATABASE IF NOT EXISTS inventory_dsa_db;
USE inventory_dsa_db;

CREATE TABLE IF NOT EXISTS products (
    product_id INT PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    category VARCHAR(80) NOT NULL,
    price DOUBLE NOT NULL,
    quantity INT NOT NULL,
    min_threshold INT NOT NULL DEFAULT 10,
    total_sold INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stock_history (
    tx_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    product_name VARCHAR(120) NOT NULL,
    action_type ENUM('ADD', 'STOCK_IN', 'STOCK_OUT', 'UPDATE', 'DELETE') NOT NULL,
    quantity_changed INT NOT NULL,
    resulting_stock INT NOT NULL,
    tx_timestamp VARCHAR(40) NOT NULL
);

INSERT IGNORE INTO products (product_id, name, category, price, quantity, min_threshold, total_sold) VALUES
(101, 'Wireless Mechanical Keyboard', 'Peripherals', 3499.00, 28, 10, 64),
(102, 'USB-C Docking Station', 'Accessories', 4299.00, 7, 10, 52),
(103, '27-Inch IPS Monitor', 'Displays', 16500.00, 14, 5, 39),
(104, 'Ergonomic Wireless Mouse', 'Peripherals', 1499.00, 4, 12, 95),
(105, 'NVMe 1TB SSD', 'Storage', 6899.00, 22, 8, 78),
(106, 'Noise Cancelling Headset', 'Audio', 5499.00, 6, 8, 44),
(107, 'Laptop Cooling Pad', 'Accessories', 1199.00, 35, 10, 31),
(108, '1080p Webcam Pro', 'Peripherals', 2799.00, 9, 10, 48);
