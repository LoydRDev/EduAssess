CREATE DATABASE IF NOT EXISTS eduassess;

USE eduassess;

-- Drop tables in reverse order of dependencies
DROP TABLE IF EXISTS instructor_evaluations;
DROP TABLE IF EXISTS grades;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS evaluation_requests;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS instructor;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS users;

-- Create users table first as it's referenced by other tables
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    id_number VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    user_type ENUM('STUDENT', 'INSTRUCTOR', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    status ENUM('ACTIVE', 'INACTIVE', 'PENDING') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create indexes for faster lookups
CREATE INDEX idx_id_number ON users(id_number);
CREATE INDEX idx_email ON users(email);

-- Insert default admin user
INSERT INTO users (full_name, email, id_number, password, user_type)
VALUES ('System Administrator', 'admin@eduassess.com', 'admin', 'admin123', 'ADMIN')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Create student table with improved student type handling
CREATE TABLE IF NOT EXISTS student (
    idStudentUser INT AUTO_INCREMENT PRIMARY KEY,
    student_IDNumber VARCHAR(50) NOT NULL UNIQUE,
    student_Name VARCHAR(100) NOT NULL,
    student_Type ENUM('REGULAR', 'TRANSFEREE', 'IRREGULAR') NOT NULL DEFAULT 'REGULAR',
    studentYearLevel ENUM('FirstYear', 'SecondYear', 'ThirdYear', 'FourthYear') NOT NULL,
    studentSemesterLevel ENUM('FirstSemester', 'SecondSemester', 'Summer') NOT NULL,
    admission_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    FOREIGN KEY (student_IDNumber) REFERENCES users(id_number) ON DELETE CASCADE
);

-- Create instructor table with department handling
CREATE TABLE IF NOT EXISTS instructor (
    id INT PRIMARY KEY AUTO_INCREMENT,
    instructor_IDNumber VARCHAR(20) NOT NULL UNIQUE,
    instructor_Name VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    specialization VARCHAR(100),
    office_location VARCHAR(50),
    contact_info VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (instructor_IDNumber) REFERENCES users(id_number) ON DELETE CASCADE
);

-- Create courses table with improved course management
CREATE TABLE IF NOT EXISTS courses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    course_Code VARCHAR(20) NOT NULL UNIQUE,
    descriptive_Title VARCHAR(100) NOT NULL,
    course_YearLevel ENUM('FirstYear', 'SecondYear', 'ThirdYear', 'FourthYear') NOT NULL,
    course_Semester ENUM('FirstSemester', 'SecondSemester', 'Summer') NOT NULL,
    instructor_IDNumber VARCHAR(20),
    lec INT NOT NULL DEFAULT 0,
    lab INT NOT NULL DEFAULT 0,
    units INT GENERATED ALWAYS AS (lec + lab) STORED,
    max_students INT DEFAULT 40,
    current_enrolled INT DEFAULT 0,
    status ENUM('AVAILABLE', 'FULL', 'CANCELLED', 'COMPLETED') DEFAULT 'AVAILABLE',
    prerequisite VARCHAR(45),
    syllabus_path VARCHAR(255),
    FOREIGN KEY (instructor_IDNumber) REFERENCES instructor(instructor_IDNumber) ON DELETE SET NULL,
    CHECK (current_enrolled <= max_students)
);

-- Create evaluation requests table
CREATE TABLE IF NOT EXISTS evaluation_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_IDNumber VARCHAR(50) NOT NULL,
    course_Code VARCHAR(50) NOT NULL,
    request_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    remarks TEXT,
    reviewed_by VARCHAR(20),
    review_date TIMESTAMP NULL,
    FOREIGN KEY (student_IDNumber) REFERENCES student(student_IDNumber) ON DELETE CASCADE,
    FOREIGN KEY (course_Code) REFERENCES courses(course_Code) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users(id_number),
    UNIQUE KEY unique_request (student_IDNumber, course_Code)
);

-- Create enrollment table with improved tracking
CREATE TABLE IF NOT EXISTS enrollments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_IDNumber VARCHAR(50) NOT NULL,
    course_Code VARCHAR(20) NOT NULL,
    year_level ENUM('FirstYear', 'SecondYear', 'ThirdYear', 'FourthYear') NOT NULL,
    semester ENUM('FirstSemester', 'SecondSemester', 'Summer') NOT NULL,
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('Enrolled', 'Dropped', 'Completed', 'Failed') DEFAULT 'Enrolled',
    drop_date TIMESTAMP NULL,
    completion_date TIMESTAMP NULL,
    FOREIGN KEY (student_IDNumber) REFERENCES student(student_IDNumber) ON DELETE CASCADE,
    FOREIGN KEY (course_Code) REFERENCES courses(course_Code) ON DELETE CASCADE,
    UNIQUE KEY unique_enrollment (student_IDNumber, course_Code, year_level, semester)
);

-- Create grades table with improved grade tracking
CREATE TABLE IF NOT EXISTS grades (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_IDNumber VARCHAR(50) NOT NULL,
    course_Code VARCHAR(20) NOT NULL,
    midterm_grade DECIMAL(5,2),
    final_grade DECIMAL(5,2),
    grade DECIMAL(5,2) NOT NULL,
    date_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    rejection_reason TEXT,
    approved_by VARCHAR(20),
    approval_date TIMESTAMP NULL,
    FOREIGN KEY (student_IDNumber) REFERENCES student(student_IDNumber) ON DELETE CASCADE,
    FOREIGN KEY (course_Code) REFERENCES courses(course_Code) ON DELETE CASCADE,
    FOREIGN KEY (approved_by) REFERENCES users(id_number),
    UNIQUE KEY unique_grade (student_IDNumber, course_Code),
    CHECK (grade >= 0 AND grade <= 100)
);

-- Create instructor evaluations table
CREATE TABLE IF NOT EXISTS instructor_evaluations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_IDNumber VARCHAR(50) NOT NULL,
    course_Code VARCHAR(20) NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments TEXT,
    evaluation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    academic_year VARCHAR(9),
    semester ENUM('FirstSemester', 'SecondSemester', 'Summer') NOT NULL,
    FOREIGN KEY (student_IDNumber) REFERENCES student(student_IDNumber) ON DELETE CASCADE,
    FOREIGN KEY (course_Code) REFERENCES courses(course_Code) ON DELETE CASCADE,
    UNIQUE KEY unique_evaluation (student_IDNumber, course_Code, academic_year, semester)
);

-- Insert the course data
INSERT INTO courses (course_Code, descriptive_Title, lec, lab, prerequisite, course_YearLevel, course_Semester) VALUES
('ENTREP 101', 'The Entrepreneurial Mind', 3, 0, 'n/a', 'FirstYear', 'FirstSemester'),
('SOCIO 101', 'The Contemporary World', 3, 0, 'n/a', 'FirstYear', 'FirstSemester'),
('MATH 101', 'Mathematics in the Modern World', 3, 0, 'n/a', 'FirstYear', 'FirstSemester'),
('CC-INTCOM11', 'Introduction to Computing', 3, 0, 'n/a', 'FirstYear', 'FirstSemester'),
('CC-COMPROG11', 'Computer Programming', 2, 1, 'n/a', 'FirstYear', 'FirstSemester'),
('IT-WEVDEV11', 'Web Design & Development', 2, 1, 'n/a', 'FirstYear', 'FirstSemester'),
('PE 101', 'Movement Competency Training (PATHFit 1)', 2, 0, 'n/a', 'FirstYear', 'FirstSemester'),
('NSTP 101', 'National Service Training Program 1', 3, 0, 'n/a', 'FirstYear', 'FirstSemester'),
('HUM 101', 'Art Appreciation', 3, 0, 'n/a', 'FirstYear', 'SecondSemester'),
('ENGL 101', 'Purposive Communication', 3, 0, 'n/a', 'FirstYear', 'SecondSemester'),
('PSYCH 101', 'Understanding the Self', 3, 0, 'n/a', 'FirstYear', 'SecondSemester'),
('HIST 101', 'Readings in Philippine History', 3, 0, 'n/a', 'FirstYear', 'SecondSemester'),
('CC-COMPROG12', 'Computer Programming 2', 2, 1, 'CC-COMPROG11', 'FirstYear', 'SecondSemester'),
('CC-DISCRET12', 'Discrete Structures', 3, 0, 'CC-INTCOM11', 'FirstYear', 'SecondSemester'),
('PE 102', 'Exercise-based Fitness Activities (PATHFit 2)', 2, 0, 'PE 101', 'FirstYear', 'SecondSemester'),
('NSTP 102', 'National Service Training Program 2', 3, 0, 'NSTP 101', 'FirstYear', 'SecondSemester'),
('STS 101', 'Science, Technology & Society', 3, 0, 'n/a', 'SecondYear', 'FirstSemester'),
('RIZAL 101', 'Life, Works & Writings of Dr. Jose Rizal', 3, 0, 'n/a', 'SecondYear', 'FirstSemester'),
('CC-DIGILOG21', 'Digital Logic Design', 2, 1, 'CC-DISCRETE12', 'SecondYear', 'FirstSemester'),
('IT-OOPROG21', 'Object Oriented Programming', 2, 1, 'CC-COMPROG12', 'SecondYear', 'FirstSemester'),
('IT-SAD21', 'System Analysis & Design', 3, 0, 'CC-COMPROG12', 'SecondYear', 'FirstSemester'),
('CC-ACCTG21', 'Accounting for IT', 3, 0, 'MATH 101', 'SecondYear', 'FirstSemester'),
('CC-TWRITE21', 'Technical Writing & Presentation Skills in IT', 3, 0, 'ENTREP 101, CC-INTCOM11', 'SecondYear', 'FirstSemester'),
('PE 103', 'Sports and Dance (PATHFit 3)', 3, 0, 'PE 102', 'SecondYear', 'FirstSemester'),
('SOCIO 102', 'Gender and Society', 3, 0, 'n/a', 'SecondYear', 'SecondSemester'),
('PHILO 101', 'Ethics', 3, 0, 'n/a', 'SecondYear', 'SecondSemester'),
('CC-QUAMETH22', 'Quantitative Methods w/ Probability Statistics', 3, 0, 'CC-DISCRETE12', 'SecondYear', 'SecondSemester'),
('IT-PLATECH22', 'Platform Technologies w/ Op. Sys.', 2, 1, 'CC-DIGILOG21', 'SecondYear', 'SecondSemester'),
('CC-APPSDEV22', 'Applications Dev''t & Emerging Tech', 2, 1, 'IT-OOPROG21, IT-SAD21', 'SecondYear', 'SecondSemester'),
('CC-DASTRUC22', 'Data Structures & Algorithms', 2, 1, 'IT-OOPROG21', 'SecondYear', 'SecondSemester'),
('CC-DATACOm22', 'Data Communications', 2, 1, 'CC-DIGILOG21', 'SecondYear', 'SecondSemester'),
('PE 104', 'Sports, Outdoor and Adventure', 2, 0, 'PE 103', 'SecondYear', 'SecondSemester'),
('IT-IMDBSYS31', 'Information Management (DB Sys. 1)', 2, 1, 'CC-APPSDEV22', 'ThirdYear', 'FirstSemester'),
('IT-NETWORK31', 'Computer Networks', 2, 1, 'CC-DATACOM22', 'ThirdYear', 'FirstSemester'),
('IT-TESTQUA31', 'Testing & Quality Assurance', 2, 1, 'CC-APPSDEV22', 'ThirdYear', 'FirstSemester'),
('CC-HCI31', 'Human Computer Interaction', 3, 0, 'CC-APPSDEV22', 'ThirdYear', 'FirstSemester'),
('CC-RESCOm31', 'Methods of Research in Computing', 3, 0, 'CC-TWRITE21, CC-QUAMETH22', 'ThirdYear', 'FirstSemester'),
('IT-EL_______', 'IT Elective 1', 2, 1, '*', 'ThirdYear', 'FirstSemester'),
('IT-FRE_____1', 'Free Elective 1', 3, 0, '**', 'ThirdYear', 'FirstSemester'),
('IT-FRE_____2', 'Free Elective 2', 3, 0, '**', 'ThirdYear', 'FirstSemester'),
('IT-IMDBSYS32', 'Information Management (DB Sys. 2)', 2, 1, 'IT-IMDBSYS31', 'ThirdYear', 'SecondSemester'),
('IT-INFOSEC32', 'Information Assurance & Security', 2, 1, 'IT-IMDBSYS31, IT-NETWORK31', 'ThirdYear', 'SecondSemester'),
('IT-SYSARCH32', 'Systems Integration & Architecture', 2, 1, 'IT-TESTQUA31, CC-HCI31', 'ThirdYear', 'SecondSemester'),
('CC-TECHNO32', 'Technopreneurship', 3, 0, 'CC-RESOM31', 'ThirdYear', 'SecondSemester'),
('IT-INTPROG32', 'Integrative Prog''g & Technologies', 2, 1, 'IT-IMDBSYS31', 'ThirdYear', 'SecondSemester'),
('IT-SYSADMIN32', 'Systems Administration & Maintenance', 2, 1, 'IT-NETWORK31', 'ThirdYear', 'SecondSemester'),
('IT-EL_____', 'IT Elective 2', 2, 1, '*', 'ThirdYear', 'SecondSemester'),
('IT-FRE_____3', 'Free Elective 3', 3, 0, '**', 'ThirdYear', 'SecondSemester'),
('LIT 101', 'Literatures of the World', 3, 0, 'n/a', 'FourthYear', 'FirstSemester'),
('IT-CPSTONE41', 'Capstone Project 1', 3, 0, '***', 'FourthYear', 'FirstSemester'),
('CC-PROFIS10', 'Professional Issues in Computing', 3, 0, '**', 'FourthYear', 'FirstSemester'),
('IT-EL______', 'IT Elective 3', 2, 1, '*', 'FourthYear', 'FirstSemester'),
('CC-PRACT40', 'Practicum', 0, 6, '***', 'FourthYear', 'SecondSemester'),
('IT-CPSTONE42', 'Capstone Project 2', 3, 0, 'IT-CPSTONE41', 'FourthYear', 'SecondSemester');

-- Create indexes for better query performance
CREATE INDEX idx_course_year_semester ON courses(course_YearLevel, course_Semester);
CREATE INDEX idx_course_instructor ON courses(instructor_IDNumber);
CREATE INDEX idx_courses_code ON courses(course_Code);
CREATE INDEX idx_enrollment_student ON enrollments(student_IDNumber);
CREATE INDEX idx_enrollment_course ON enrollments(course_Code);
CREATE INDEX idx_enrollment_status ON enrollments(status);
CREATE INDEX idx_grades_student ON grades(student_IDNumber);
CREATE INDEX idx_grades_course ON grades(course_Code);
CREATE INDEX idx_grades_status ON grades(status);
CREATE INDEX idx_student_evaluation ON instructor_evaluations(student_IDNumber);
CREATE INDEX idx_course_evaluation ON instructor_evaluations(course_Code);

