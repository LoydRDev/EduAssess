CREATE TABLE IF NOT EXISTS courses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    course_Code VARCHAR(20) NOT NULL,
    descriptive_Title VARCHAR(100) NOT NULL,
    course_YearLevel VARCHAR(20) NOT NULL,
    course_Semester VARCHAR(20) NOT NULL,
    instructor_IDNumber VARCHAR(20),
    lec INT NOT NULL DEFAULT 0,
    lab INT NOT NULL DEFAULT 0,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    prerequisite VARCHAR(20)
    FOREIGN KEY (instructor_IDNumber) REFERENCES instructor(instructor_IDNumber) ON DELETE SET NULL
);

-- Create indexes for faster lookups
CREATE INDEX idx_course_year_semester ON courses(course_YearLevel, course_Semester);
CREATE INDEX idx_course_instructor ON courses(instructor_IDNumber);

-- Create enrollments table for many-to-many relationship
CREATE TABLE IF NOT EXISTS enrollments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_IDNumber VARCHAR(50) NOT NULL,
    course_id INT NOT NULL,
    status VARCHAR(20) DEFAULT 'ENROLLED',
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_IDNumber) REFERENCES student(student_IDNumber),
    FOREIGN KEY (course_id) REFERENCES courses(id)
);

-- Create indexes for faster lookups
CREATE INDEX idx_student_enrollment ON enrollments(student_IDNumber);
CREATE INDEX idx_course_enrollment ON enrollments(course_id);

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
('IT-FRE______', 'Free Elective 1', 3, 0, '**', 'ThirdYear', 'FirstSemester'),
('IT-FRE______', 'Free Elective 2', 3, 0, '**', 'ThirdYear', 'FirstSemester'),
('IT-IMDBSYS32', 'Information Management (DB Sys. 2)', 2, 1, 'IT-IMDBSYS31', 'ThirdYear', 'SecondSemester'),
('IT-INFOSEC32', 'Information Assurance & Security', 2, 1, 'IT-IMDBSYS31, IT-NETWORK31', 'ThirdYear', 'SecondSemester'),
('IT-SYSARCH32', 'Systems Integration & Architecture', 2, 1, 'IT-TESTQUA31, CC-HCI31', 'ThirdYear', 'SecondSemester'),
('CC-TECHNO32', 'Technopreneurship', 3, 0, 'CC-RESOM31', 'ThirdYear', 'SecondSemester'),
('IT-INTPROG32', 'Integrative Prog''g & Technologies', 2, 1, 'IT-IMDBSYS31', 'ThirdYear', 'SecondSemester'),
('IT-SYSADMIN32', 'Systems Administration & Maintenance', 2, 1, 'IT-NETWORK31', 'ThirdYear', 'SecondSemester'),
('IT-EL_____', 'IT Elective 2', 2, 1, '*', 'ThirdYear', 'SecondSemester'),
('IT-FRE_____', 'Free Elective 3', 3, 0, '**', 'ThirdYear', 'SecondSemester'),
('LIT 101', 'Literatures of the World', 3, 0, 'n/a', 'FourthYear', 'FirstSemester'),
('IT-CPSTONE41', 'Capstone Project 1', 3, 0, '***', 'FourthYear', 'FirstSemester'),
('CC-PROFIS10', 'Professional Issues in Computing', 3, 0, '**', 'FourthYear', 'FirstSemester'),
('IT-EL______', 'IT Elective 3', 2, 1, '*', 'FourthYear', 'FirstSemester'),
('CC-PRACT40', 'Practicum', 0, 6, '***', 'FourthYear', 'SecondSemester'),
('IT-CPSTONE42', 'Capstone Project 2', 3, 0, 'IT-CPSTONE41', 'FourthYear', 'SecondSemester');