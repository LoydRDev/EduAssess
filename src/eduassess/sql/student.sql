CREATE TABLE IF NOT EXISTS student (
    idStudentUser INT AUTO_INCREMENT PRIMARY KEY,
    student_IDNumber VARCHAR(50) NOT NULL,
    student_Name VARCHAR(45) NOT NULL,
    student_Type VARCHAR(45) NOT NULL,
    student_Email VARCHAR(45) NOT NULL,
    studentYearLevel VARCHAR(45),
    studentSemesterLevel VARCHAR(45),
    FOREIGN KEY (student_IDNumber) REFERENCES users(id_number),
    FOREIGN KEY (student_Name) REFERENCES users(full_name),
    FOREIGN KEY (student_Email) REFERENCES users (email)
);