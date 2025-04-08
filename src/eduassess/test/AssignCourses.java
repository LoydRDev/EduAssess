package eduassess.test;

import eduassess.dao.CourseDAO;

public class AssignCourses {
    public static void main(String[] args) {
        CourseDAO courseDAO = new CourseDAO();
        String instructorId = "123123";

        boolean success = courseDAO.assignAllCoursesToInstructor(instructorId);
        if (success) {
            System.out.println("Successfully assigned all courses to instructor " + instructorId);
        } else {
            System.out.println("Failed to assign courses to instructor " + instructorId);
        }
    }
}