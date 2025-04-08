package eduassess.test;

import eduassess.dao.CourseDAO;

public class TestAssignCourses {
    public static void main(String[] args) {
        CourseDAO dao = new CourseDAO();
        boolean success = dao.assignAllCoursesToInstructor("123123");
        System.out.println("Assignment successful: " + success);
    }
}