DROP TABLE IF EXISTS instructor_evaluations;

CREATE TABLE IF NOT EXISTS instructor_evaluations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_IDNumber VARCHAR(50) NOT NULL,
    course_Code VARCHAR(20) NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments TEXT,
    evaluation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_IDNumber) REFERENCES student(student_IDNumber) ON DELETE CASCADE,
    FOREIGN KEY (course_Code) REFERENCES courses(course_Code) ON DELETE CASCADE,
    UNIQUE KEY unique_evaluation (student_IDNumber, course_Code)
);

-- Create indexes for faster lookups
CREATE INDEX idx_student_evaluation ON instructor_evaluations(student_IDNumber);
CREATE INDEX idx_course_evaluation ON instructor_evaluations(course_Code); 