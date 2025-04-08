package eduassess.controller;

import eduassess.model.User;
import eduassess.model.Course;
import eduassess.model.CourseEnrollment;
import eduassess.dao.CourseDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.application.Platform;
import java.util.Objects;
import java.util.Optional;

public class RecommendationsController {
    @FXML
    private TableView<Course> recommendedCoursesTable;
    @FXML
    private TableColumn<Course, String> courseCodeColumn;
    @FXML
    private TableColumn<Course, String> courseTitleColumn;
    @FXML
    private TableColumn<Course, String> prerequisitesColumn;
    @FXML
    private TableColumn<Course, Integer> unitsColumn;
    @FXML
    private TableColumn<Course, Double> recommendationScoreColumn;
    @FXML
    private TableColumn<Course, String> enrollColumn;

    private User currentUser;
    private CourseDAO courseDAO;
    private String nextSemester;
    private String nextYearLevel;

    public RecommendationsController() {
        courseDAO = new CourseDAO();
    }

    public void initData(User user) {
        if (user == null) {
            showError("Error", "No user data available");
            return;
        }

        this.currentUser = user;

        // Check if all courses have been evaluated and there are no pending evaluations
        List<CourseEnrollment> courses = courseDAO.getStudentCourses(user.getIdNumber());
        boolean hasUnevaluatedCourses = courses.stream()
                .anyMatch(course -> course.getGrade() == null || course.getGrade().isEmpty());
        boolean hasPendingEvaluations = courses.stream()
                .anyMatch(CourseEnrollment::isPending);

        if (hasUnevaluatedCourses || hasPendingEvaluations) {
            String message = hasPendingEvaluations
                    ? "Please wait for pending course evaluations to be completed before accessing recommendations."
                    : "Please ensure all your courses have been evaluated before accessing recommendations.";
            showError("Access Denied", message);
            handleDashboard(); // Redirect back to dashboard
            return;
        }

        setupTable();
        loadRecommendations();
    }

    private void setupTable() {
        try {
            // Initialize table columns with null checks
            if (courseCodeColumn != null)
                courseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
            if (courseTitleColumn != null)
                courseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
            if (prerequisitesColumn != null) {
                prerequisitesColumn.setCellValueFactory(new PropertyValueFactory<>("prerequisites"));
                prerequisitesColumn.setCellFactory(column -> new TableCell<Course, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null || item.trim().isEmpty() || item.equalsIgnoreCase("n/a")) {
                            setText("None");
                        } else {
                            setText(item);
                        }
                    }
                });
            }
            if (unitsColumn != null)
                unitsColumn.setCellValueFactory(new PropertyValueFactory<>("units"));
            if (recommendationScoreColumn != null)
                recommendationScoreColumn.setCellValueFactory(new PropertyValueFactory<>("recommendationScore"));

            // Format recommendation score column to show 2 decimal places
            recommendationScoreColumn.setCellFactory(column -> new TableCell<Course, Double>() {

                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("%.2f", item));
                    }
                }
            });

            // Configure enrollment column with button cell factory
            enrollColumn.setCellFactory(col -> new TableCell<Course, String>() {
                private final Button enrollButton = new Button("Enroll");
                {
                    enrollButton.setOnAction(event -> {
                        Course course = getTableView().getItems().get(getIndex());
                        if (course != null) {
                            handleEnrollment(course);
                        }
                    });
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        Course course = getTableView().getItems().get(getIndex());
                        if (course != null && course.isFailedCourse()) {
                            // Change button text to Re-enroll for failed courses
                            enrollButton.setText("Re-enroll");
                            // Add a different style for re-enrollment button
                            enrollButton.setStyle(
                                    "-fx-background-color: #ff4444;" + // Red color for re-enrollment
                                            "-fx-text-fill: white;" +
                                            "-fx-font-weight: bold;");
                        } else {
                            enrollButton.setText("Enroll");
                            enrollButton.setStyle(""); // Reset to default style
                        }
                        setGraphic(enrollButton);
                    }
                }
            });

            // Set table style and properties
            if (recommendedCoursesTable != null) {
                recommendedCoursesTable.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-border-color: #e2e8f0;" +
                                "-fx-border-radius: 4px;");

                recommendedCoursesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            }
        } catch (

        Exception e) {
            e.printStackTrace();
            showError("Error", "Failed to setup table: " + e.getMessage());
        }
    }

    private String getNextYearLevel(String currentYearLevel) {
        switch (currentYearLevel) {
            case "FirstYear":
                return "SecondYear";
            case "SecondYear":
                return "ThirdYear";
            case "ThirdYear":
                return "FourthYear";
            default:
                return currentYearLevel;
        }
    }

    private void handleEnrollment(Course course) {
        if (currentUser == null || course == null) {
            showError("Error", "Invalid course or user data");
            return;
        }

        try {
            // Verify prerequisites again before enrollment
            List<CourseEnrollment> enrolledCourses = courseDAO.getStudentCourses(currentUser.getIdNumber());

            // Skip prerequisite check for failed courses that are being re-enrolled
            if (!course.isFailedCourse() && !hasCompletedPrerequisites(course.getPrerequisites(), enrolledCourses)) {
                showError("Enrollment Error", "Prerequisites not met for this course");
                return;
            }

            String enrollmentYearLevel;
            String enrollmentSemester;

            if (course.isFailedCourse()) {
                // For failed courses, use the same year level and semester as the original
                // enrollment
                // Find the failed course enrollment to get its year level and semester
                Optional<CourseEnrollment> failedEnrollment = enrolledCourses.stream()
                        .filter(e -> e.getCourseCode().equals(course.getCourseCode()))
                        .findFirst();

                if (failedEnrollment.isPresent()) {
                    enrollmentYearLevel = failedEnrollment.get().getYearLevel();
                    enrollmentSemester = failedEnrollment.get().getSemester();
                } else {
                    showError("Error", "Could not find original enrollment for failed course");
                    return;
                }
            } else {
                // For new courses, calculate next semester and year level
                CourseEnrollment latestEnrollment = enrolledCourses.get(enrolledCourses.size() - 1);
                String currentYearLevel = latestEnrollment.getYearLevel();
                String currentSemester = latestEnrollment.getSemester();

                if (currentSemester.equals("FirstSemester")) {
                    enrollmentSemester = "SecondSemester";
                    enrollmentYearLevel = currentYearLevel;
                } else {
                    enrollmentSemester = "FirstSemester";
                    enrollmentYearLevel = getNextYearLevel(currentYearLevel);
                }
            }

            // Enroll student in the selected course
            boolean success = courseDAO.enrollStudentInCourse(currentUser.getIdNumber(),
                    course.getCourseCode(),
                    enrollmentYearLevel,
                    enrollmentSemester);

            if (success) {
                Platform.runLater(() -> {
                    String message = course.isFailedCourse()
                            ? String.format("Successfully re-enrolled in %s for %s - %s",
                                    course.getCourseCode(),
                                    formatYearLevel(enrollmentYearLevel),
                                    formatSemester(enrollmentSemester))
                            : String.format("Successfully enrolled in %s for %s - %s",
                                    course.getCourseCode(),
                                    formatYearLevel(enrollmentYearLevel),
                                    formatSemester(enrollmentSemester));

                    showAlert(Alert.AlertType.INFORMATION, "Success", message);
                    // Refresh recommendations after successful enrollment
                    loadRecommendations();
                });
            } else {
                showError("Error", "Failed to enroll in course");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "An error occurred while enrolling in the course");
        }
    }

    private String formatYearLevel(String yearLevel) {
        return switch (yearLevel) {
            case "FirstYear" -> "First Year";
            case "SecondYear" -> "Second Year";
            case "ThirdYear" -> "Third Year";
            case "FourthYear" -> "Fourth Year";
            default -> yearLevel;
        };
    }

    private String formatSemester(String semester) {
        return switch (semester) {
            case "FirstSemester" -> "First Semester";
            case "SecondSemester" -> "Second Semester";
            default -> semester;
        };
    }

    @FXML
    public void initialize() {
        setupTable();
    }

    private void loadRecommendations() {
        if (currentUser == null) {
            showError("Error", "No user data available");
            return;
        }

        try {
            List<CourseEnrollment> enrolledCourses = courseDAO.getStudentCourses(currentUser.getIdNumber());
            if (enrolledCourses == null || enrolledCourses.isEmpty()) {
                recommendedCoursesTable.setItems(FXCollections.observableArrayList());
                showAlert(Alert.AlertType.INFORMATION, "No Courses", "You are not currently enrolled in any courses.");
                return;
            }

            List<Course> recommendedCourses = new ArrayList<>();

            // Check for failed courses first
            List<Course> failedCourses = enrolledCourses.stream()
                    .filter(course -> {
                        String grade = course.getGrade();
                        if (grade == null || grade.equals("Not Graded")) {
                            return false;
                        }
                        try {
                            // Check if it's a numeric grade
                            if (grade.matches("\\d+(\\.\\d+)?")) {
                                double numericGrade = Double.parseDouble(grade);
                                // Handle both 1.0-3.0 scale and 75-100 scale
                                if (numericGrade >= 1.0 && numericGrade <= 5.0) {
                                    // Philippine grading system: 1.0 is highest, 3.0 is passing
                                    return numericGrade > 3.0; // Failed if grade is greater than 3.0
                                } else if (numericGrade >= 0 && numericGrade <= 100) {
                                    // Percentage-based grading: 75-100 is passing
                                    return numericGrade < 75.0; // Failed if grade is less than 75
                                }
                            }
                            // For non-numeric grades
                            String upperGrade = grade.toUpperCase();
                            return upperGrade.equals("F") || upperGrade.equals("FAILED") ||
                                    upperGrade.equals("INC") || upperGrade.equals("INCOMPLETE") ||
                                    upperGrade.equals("DRP") || upperGrade.equals("DROPPED");
                        } catch (NumberFormatException e) {
                            // For any other text-based grades
                            String upperGrade = grade.toUpperCase();
                            return upperGrade.equals("F") || upperGrade.equals("FAILED") ||
                                    upperGrade.equals("INC") || upperGrade.equals("INCOMPLETE") ||
                                    upperGrade.equals("DRP") || upperGrade.equals("DROPPED");
                        }
                    })
                    .map(course -> {
                        try {
                            Course failedCourse = new Course(
                                    course.getCourseCode(),
                                    course.getCourseName(),
                                    course.getPrerequisites(),
                                    Integer.parseInt(course.getUnits()),
                                    1.0, // highest priority for failed courses
                                    course.getYearLevel(),
                                    course.getSemester());
                            failedCourse.setFailedCourse(true); // Mark as a failed course
                            System.out.println("Found failed course: " + course.getCourseCode() + " - "
                                    + course.getCourseName() + " (Grade: " + course.getGrade() + ")");
                            return failedCourse;
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!failedCourses.isEmpty()) {
                recommendedCourses.addAll(failedCourses);
                showAlert(Alert.AlertType.INFORMATION, "Course Recommendations",
                        "There are failed or incomplete courses that need to be retaken.");
            } else {
                // Get latest enrollment to determine next semester/year
                CourseEnrollment latestEnrollment = enrolledCourses.get(enrolledCourses.size() - 1);
                String currentYearLevel = latestEnrollment.getYearLevel();
                String currentSemester = latestEnrollment.getSemester();

                if (currentYearLevel == null || currentSemester == null) {
                    showError("Error", "Invalid year level or semester information");
                    return;
                }

                // Determine next semester and year level
                if (currentSemester.equals("FirstSemester")) {
                    nextSemester = "SecondSemester";
                    nextYearLevel = currentYearLevel;
                } else {
                    // If it's second semester, move to the next year's first semester
                    nextSemester = "FirstSemester";
                    nextYearLevel = getNextYearLevel(currentYearLevel);
                }

                System.out.println("Current Year Level: " + currentYearLevel);
                System.out.println("Current Semester: " + currentSemester);
                System.out.println("Next Year Level: " + nextYearLevel);
                System.out.println("Next Semester: " + nextSemester);

                // Get available courses for next semester only
                List<Course> availableCourses = courseDAO.getAvailableCourses(
                        currentUser.getIdNumber(), nextYearLevel, nextSemester);

                System.out.println("Found " + (availableCourses != null ? availableCourses.size() : 0) +
                        " available courses for next semester");

                if (availableCourses == null || availableCourses.isEmpty()) {
                    showAlert(Alert.AlertType.INFORMATION, "No Available Courses",
                            "There are no available courses for the next semester at this time.");
                    return;
                }

                // Filter out courses that the student has already passed
                List<String> passedCourseCodes = enrolledCourses.stream()
                        .filter(ec -> isPassingGrade(ec.getGrade()))
                        .map(CourseEnrollment::getCourseCode)
                        .collect(Collectors.toList());

                availableCourses = availableCourses.stream()
                        .filter(course -> !passedCourseCodes.contains(course.getCourseCode()))
                        .collect(Collectors.toList());

                if (availableCourses.isEmpty()) {
                    showAlert(Alert.AlertType.INFORMATION, "No Available Courses",
                            "There are no new courses available for the next semester. You have already passed all available courses.");
                    return;
                }

                // Calculate recommendation scores and sort all available courses
                List<Course> nextLevelCourses = availableCourses.stream()
                        .map(course -> {
                            try {
                                boolean prereqsMet = hasCompletedPrerequisites(course.getPrerequisites(),
                                        enrolledCourses);
                                double score = calculateRecommendationScore(course, enrolledCourses);
                                // Adjust score based on prerequisites
                                if (!prereqsMet) {
                                    score *= 0.5; // Lower priority for courses with unmet prerequisites
                                }
                                course.setRecommendationScore(score);
                                return course;
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .sorted((c1, c2) -> Double.compare(c2.getRecommendationScore(), c1.getRecommendationScore()))
                        .collect(Collectors.toList());

                if (!nextLevelCourses.isEmpty()) {
                    recommendedCourses.addAll(nextLevelCourses);
                    showAlert(Alert.AlertType.INFORMATION, "Course Recommendations",
                            String.format("Found %d recommended courses for next semester.", nextLevelCourses.size()));
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "No Recommendations",
                            "No suitable courses found for the next semester. Please consult with your advisor.");
                }
            }

            // Update the table with recommended courses
            ObservableList<Course> observableList = FXCollections.observableArrayList(recommendedCourses);
            Platform.runLater(() -> {
                recommendedCoursesTable.setItems(observableList);
                if (!recommendedCourses.isEmpty()) {
                    showAlert(Alert.AlertType.INFORMATION, "Course Recommendations",
                            "Please select individual courses to enroll in by clicking the 'Enroll' button next to each course.");
                }
            });

        } catch (

        Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                recommendedCoursesTable.setItems(FXCollections.observableArrayList());
                showError("Error", "Failed to load course recommendations: " + e.getMessage());
            });
        }
    }

    @FXML
    private void handleDashboard() {
        try {
            if (recommendedCoursesTable != null) {
                recommendedCoursesTable.setItems(null);
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/eduassess/views/StudentDashboard.fxml"));
            Parent dashboardView = loader.load();
            StudentDashboardController controller = loader.getController();
            controller.initData(currentUser);

            Stage stage = (Stage) recommendedCoursesTable.getScene().getWindow();
            Scene scene = new Scene(dashboardView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error", "Failed to return to dashboard");
        }
    }

    private double calculateRecommendationScore(Course course, List<CourseEnrollment> enrolledCourses) {
        if (course == null || enrolledCourses == null) {
            return 0.0;
        }

        // If it's a failed course, give it highest priority
        if (course.isFailedCourse()) {
            return 1.0;
        }

        double score = 0.3; // Base score

        try {
            // Check prerequisites completion
            String prerequisites = course.getPrerequisites();
            if (prerequisites != null && !prerequisites.trim().isEmpty() && !prerequisites.equalsIgnoreCase("n/a")) {
                String[] prereqArray = prerequisites.split(",");
                int completedPrereqs = 0;

                for (String prereq : prereqArray) {
                    final String currentPrereq = prereq.trim();
                    if (!currentPrereq.isEmpty()) {
                        boolean recentlyCompleted = enrolledCourses.stream()
                                .filter(ec -> ec != null && ec.getCourseCode() != null)
                                .anyMatch(ec -> ec.getCourseCode().equals(currentPrereq) &&
                                        ec.getGrade() != null &&
                                        isPassingGrade(ec.getGrade()));
                        if (recentlyCompleted) {
                            completedPrereqs++;
                        }
                    }
                }

                if (prereqArray.length > 0) {
                    // Higher weight for prerequisites completion
                    score += 0.3 * ((double) completedPrereqs / prereqArray.length);
                }
            } else {
                // Bonus for courses without prerequisites
                score += 0.2;
            }

            // Course type priority
            String courseCode = course.getCourseCode();
            if (courseCode != null) {
                if (courseCode.startsWith("MAJOR")) {
                    score += 0.25; // Prioritize major courses
                } else if (courseCode.startsWith("CORE")) {
                    score += 0.15;
                } else if (courseCode.startsWith("GE")) {
                    score += 0.1;
                }
            }

            // Add semester relevance - prioritize courses for the immediate next semester
            if (course.getYearLevel().equals(nextYearLevel) && course.getSemester().equals(nextSemester)) {
                score += 0.2; // Boost score for courses in the next semester
            }

            return Math.min(score, 0.95); // Cap non-failed courses at 0.95
        } catch (Exception e) {
            e.printStackTrace();
            return 0.5;
        }
    }

    private boolean hasCompletedPrerequisites(String prerequisites, List<CourseEnrollment> enrolledCourses) {
        if (prerequisites == null || prerequisites.trim().isEmpty() || prerequisites.equalsIgnoreCase("n/a")) {
            return true; // No prerequisites needed
        }

        if (enrolledCourses == null || enrolledCourses.isEmpty()) {
            return false; // Cannot verify prerequisites without enrollment history
        }

        return Arrays.stream(prerequisites.split(","))
                .map(String::trim)
                .filter(prereq -> !prereq.isEmpty())
                .allMatch(prereq -> enrolledCourses.stream()
                        .filter(ec -> ec != null && ec.getCourseCode() != null && ec.getGrade() != null)
                        .anyMatch(ec -> ec.getCourseCode().equals(prereq) && isPassingGrade(ec.getGrade())));
    }

    private boolean isPassingGrade(String grade) {
        if (grade == null || grade.equals("Not Graded") || grade.equals("PENDING")) {
            return true; // Ungraded courses should not be considered as failed
        }

        // Check if it's a numeric grade
        if (grade.matches("\\d+(\\.\\d+)?")) {
            try {
                double numericGrade = Double.parseDouble(grade);
                // Handle both 1.0-3.0 scale and 75-100 scale
                if (numericGrade >= 1.0 && numericGrade <= 5.0) {
                    // Philippine grading system: 1.0 is highest, 3.0 is passing
                    return numericGrade <= 3.0; // Passing if grade is 3.0 or less
                } else if (numericGrade >= 0 && numericGrade <= 100) {
                    // Percentage-based grading: 75-100 is passing
                    return numericGrade >= 75.0; // Passing if grade is 75 or higher
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // Handle special grades
        return !grade.equals("FAILED") && !grade.equals("DRP") && !grade.equals("INC");
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Style the alert dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: #ff4444; -fx-border-width: 2px;");

        // Style the buttons
        dialogPane.lookupButton(ButtonType.OK).setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");

        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        try {
            if (recommendedCoursesTable != null) {
                recommendedCoursesTable.setItems(null);
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/eduassess/views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) recommendedCoursesTable.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error", "Failed to logout. Please try again.");
        }
    }
}