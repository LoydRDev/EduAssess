CREATE TABLE IF NOT EXISTS instructor (
    id INT PRIMARY KEY AUTO_INCREMENT,
    instructor_IDNumber VARCHAR(20) NOT NULL UNIQUE,
    instructor_Name VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (instructor_IDNumber) REFERENCES users(id_number) ON DELETE CASCADE
);

-- Create index for faster lookups
CREATE INDEX idx_instructor_id_number ON instructor(instructor_IDNumber); 