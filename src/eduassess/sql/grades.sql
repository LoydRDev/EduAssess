-- Add index to courses table
CREATE INDEX idx_courses_code ON courses(course_Code);

CREATE TABLE IF NOT EXISTS grades (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_IDNumber VARCHAR(20) NOT NULL,
    course_Code VARCHAR(20) NOT NULL,
    grade DECIMAL(5,2) NOT NULL,
    date_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    rejection_reason TEXT,
    FOREIGN KEY (student_IDNumber) REFERENCES student(student_IDNumber) ON DELETE CASCADE,
    FOREIGN KEY (course_Code) REFERENCES courses(course_Code) ON DELETE CASCADE,
    UNIQUE KEY unique_grade (student_IDNumber, course_Code)
);

-- Create indexes for faster lookups
CREATE INDEX idx_grades_student ON grades(student_IDNumber);
CREATE INDEX idx_grades_course ON grades(course_Code);
CREATE INDEX idx_grades_status ON grades(status); 