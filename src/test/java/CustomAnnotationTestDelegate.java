import model.Employee;
import validator.ValidFilter;

public class CustomAnnotationTestDelegate {

    public void validateFilterMethod(@ValidFilter(clazz = Employee.class)final String filter) {
    }
}