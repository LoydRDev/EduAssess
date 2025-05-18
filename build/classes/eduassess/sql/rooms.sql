CREATE TABLE IF NOT EXISTS rooms (
    room_id INT AUTO_INCREMENT PRIMARY KEY,
    room_name VARCHAR(50) NOT NULL,
    room_number VARCHAR(10) NOT NULL UNIQUE,
    building VARCHAR(50) NOT NULL,
    capacity INT NOT NULL,
    status ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE') DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Sample data
INSERT INTO rooms (room_number, building, capacity, status) VALUES
('101', 'Main Building', 30, 'AVAILABLE'),
('201', 'Science Building', 25, 'AVAILABLE'),
('301', 'Arts Building', 20, 'MAINTENANCE');