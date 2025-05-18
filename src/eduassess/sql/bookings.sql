CREATE TABLE IF NOT EXISTS bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL,
    user_id INT NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    purpose VARCHAR(100) NOT NULL,
    booking_date DATE NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    timeslot ENUM('MORNING', 'AFTERNOON', 'EVENING') NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_code) REFERENCES courses(course_Code) ON DELETE CASCADE
);

-- Create index for faster lookups
CREATE INDEX idx_booking_room ON bookings(room_id);
CREATE INDEX idx_booking_user ON bookings(user_id);
CREATE INDEX idx_booking_date ON bookings(booking_date);