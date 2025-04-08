-- Create the evaluation_requests table
CREATE TABLE IF NOT EXISTS evaluation_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_IDNumber VARCHAR(50) NOT NULL,
    course_Code VARCHAR(50) NOT NULL,
    request_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    FOREIGN KEY (student_IDNumber) REFERENCES student(student_IDNumber) ON DELETE CASCADE,
    FOREIGN KEY (course_Code) REFERENCES courses(course_Code) ON DELETE CASCADE,
    UNIQUE KEY unique_request (student_IDNumber, course_Code)
);

-- Create indexes for better query performance
CREATE INDEX idx_evaluation_requests_student ON evaluation_requests(student_IDNumber);
CREATE INDEX idx_evaluation_requests_course ON evaluation_requests(course_Code);
CREATE INDEX idx_evaluation_requests_status ON evaluation_requests(status); 