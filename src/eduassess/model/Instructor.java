
package eduassess.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;

public class Instructor {
    private final IntegerProperty id;
    private final IntegerProperty idNumber;
    private final StringProperty fullName;
    private final StringProperty course_Code;
    private final StringProperty course_Name;
    private final StringProperty status;

    public Instructor(Integer idNumber, String fullName, String course_Code, String course_Name) {
        this.id = new SimpleIntegerProperty(0);
        this.idNumber = new SimpleIntegerProperty(idNumber);
        this.fullName = new SimpleStringProperty(fullName);
        this.course_Code = new SimpleStringProperty(course_Code);
        this.course_Name = new SimpleStringProperty(course_Name); 
        this.status = new SimpleStringProperty("Active");
    }

    //getters property
    public IntegerProperty idProperty() {
        return id;
    }

    public IntegerProperty idNumberProperty() {
        return idNumber;
    }

    public StringProperty fullNameProperty() {
        return fullName;
    }

    public StringProperty course_CodeProperty(){
        return course_Code;
    }
    
    public StringProperty course_NameProperty(){
        return course_Name;
    }
    
    public StringProperty statusPropertyProperty() {
        return status;
    }

    //getters
    public int getId() {
        return id.get();
    }

    public Integer getIdNumber() {
        return idNumber.get();
    }

    public String getFullName() {
        return fullName.get();
    }
    
    public String getCourse_Code(){
        return course_Code.get();
    }

    public String getCourse_Name(){
        return course_Name.get();
    }
    
    public String getStatus() {
        return status.get();
    }
    
    //setters
    public void setIdNumber(int idNumber){
        this.idNumber.set(idNumber);
    }
    
    public void setInstructorName(String fullName){
        this.fullName.set(fullName);
    }
    
    public void setCourse_Code(String course_Code){
        this.course_Code.set(course_Code);
    }
    
    public void setCourse_Name(String course_Name){
        this.course_Name.set(course_Name);
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
