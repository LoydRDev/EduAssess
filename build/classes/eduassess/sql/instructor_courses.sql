CREATE TABLE IF NOT EXISTS instructor_courses (
    id INT PRIMARY KEY AUTO_INCREMENT,
    instructor_IDNumber VARCHAR(20) NOT NULL,
    course_Code VARCHAR(20) NOT NULL,
    assigned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('Active', 'Inactive') DEFAULT 'Active',
    FOREIGN KEY (instructor_IDNumber) REFERENCES users(id_number) ON DELETE CASCADE,
    FOREIGN KEY (course_Code) REFERENCES courses(course_Code) ON DELETE CASCADE,
    UNIQUE KEY unique_instructor_course (instructor_IDNumber, course_Code)
);

-- Create indexes for better query performance
CREATE INDEX idx_instructor_course ON instructor_courses(instructor_IDNumber, course_Code);
CREATE INDEX idx_instructor_status ON instructor_courses(status);