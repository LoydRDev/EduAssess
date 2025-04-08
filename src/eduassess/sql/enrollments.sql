CREATE TABLE IF NOT EXISTS enrollments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_IDNumber VARCHAR(20) NOT NULL,
    course_Code VARCHAR(20) NOT NULL,
    year_level ENUM('FirstYear', 'SecondYear', 'ThirdYear', 'FourthYear') NOT NULL,
    semester ENUM('FirstSemester', 'SecondSemester', 'Summer') NOT NULL,
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('Enrolled', 'Dropped', 'Completed') DEFAULT 'Enrolled',
    FOREIGN KEY (student_IDNumber) REFERENCES student(student_IDNumber) ON DELETE CASCADE,
    FOREIGN KEY (course_Code) REFERENCES courses(course_Code) ON DELETE CASCADE,
    UNIQUE KEY unique_enrollment (student_IDNumber, course_Code, year_level, semester)
);

-- Create indexes for better query performance
CREATE INDEX idx_enrollment_student ON enrollments(student_IDNumber);
CREATE INDEX idx_enrollment_course ON enrollments(course_Code);
CREATE INDEX idx_enrollment_status ON enrollments(status);