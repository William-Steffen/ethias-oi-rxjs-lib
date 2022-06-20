import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import model.Employee;
import model.Person;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidFilterTest {

    private final CustomAnnotationTestDelegate testDelegate = new CustomAnnotationTestDelegate();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ExecutableValidator executableValidator = validator.forExecutables();
    private final String unknownFieldResource = "UnknownField";
    private final String unknownFieldMessage = "ethias.oi.rxjs.ValidFilter.unknownField";
    private final String invalidSyntaxResource = "InvalidSyntax";
    private final String invalidSyntaxMessage = "ethias.oi.rxjs.ValidFilter.invalidSyntax";
    private final String invalidNestedObjectResource = "InvalidNestedObject";
    private final String invalidNestedObjectMessage = "ethias.oi.rxjs.ValidFilter.invalidNestedObject";

    @Test
    public void testValidFilter() {
        final String filter = "age==12 and firstname==William or lastname==Dupuis";

        Optional<ConstraintViolation<CustomAnnotationTestDelegate>> violations = validate(filter);

        assertTrue(violations.isEmpty());
    }

    @Test
    public void testInvalidSyntax() {
        final String filter = "age$";
        Optional<ConstraintViolation<CustomAnnotationTestDelegate>> violation = validate(filter);
        assertTrue(violation.isPresent());
        assertTrue(validateConstraintViolationMessage(invalidSyntaxResource, invalidSyntaxMessage, violation.get()));
    }

    @Test
    public void testInvalidSyntaxWithLogicalNodes() {
        final String filter = "age==12 and firstname==William or lastname";
        Optional<ConstraintViolation<CustomAnnotationTestDelegate>> violation = validate(filter);
        assertTrue(violation.isPresent());
        assertTrue(validateConstraintViolationMessage(invalidSyntaxResource, invalidSyntaxMessage, violation.get()));
    }

    @Test
    public void testUnknownField() {
        final String invalidField = "fistname";
        final String filter = invalidField + " == William";
        Optional<ConstraintViolation<CustomAnnotationTestDelegate>> violation = validate(filter);
        assertTrue(violation.isPresent());
        assertTrue(validateConstraintViolationMessage(
                unknownFieldResource,
                unknownFieldMessage,
                violation.get(),
                invalidField,
                Employee.class.getSimpleName()));
    }

    @Test
    public void testUnknownFieldWithLogicalNodes() {
        final String invalidField = "fistname";
        final String filter = invalidField + " == William and lastname==Steffen";
        Optional<ConstraintViolation<CustomAnnotationTestDelegate>> violation = validate(filter);
        assertTrue(violation.isPresent());
        assertTrue(validateConstraintViolationMessage(
                unknownFieldResource,
                unknownFieldMessage,
                violation.get(),
                invalidField,
                Employee.class.getSimpleName()));
    }

    @Test
    public void testInvalidNestedObjectField() {
        final String invalidField = "postalCde";
        final String filter = "address." + invalidField + "==4000";
        Optional<ConstraintViolation<CustomAnnotationTestDelegate>> violation = validate(filter);
        assertTrue(violation.isPresent());
        assertTrue(validateConstraintViolationMessage(
                unknownFieldResource,
                unknownFieldMessage,
                violation.get(),
                invalidField,
                Employee.class.getSimpleName()));
    }

    @Test
    public void testInvalidNestedObjectFieldWithLogicalNodes() {
        final String invalidField = "postalCde";
        final String filter = "firstname==Steffen or address." + invalidField + "==4000";
        Optional<ConstraintViolation<CustomAnnotationTestDelegate>> violation = validate(filter);
        assertTrue(violation.isPresent());
        assertTrue(validateConstraintViolationMessage(
                unknownFieldResource,
                unknownFieldMessage,
                violation.get(),
                invalidField,
                Employee.class.getSimpleName()));
    }

    @Test
    public void testInvalidNestedObject() {
        final String invalidField = "address";
        final String filter = invalidField + "==4000";
        Optional<ConstraintViolation<CustomAnnotationTestDelegate>> violation = validate(filter);
        assertTrue(violation.isPresent());
        assertTrue(validateConstraintViolationMessage(
                invalidNestedObjectResource,
                invalidNestedObjectMessage,
                violation.get(),
                invalidField,
                Person.class.getSimpleName()));
    }

    @Test
    public void testInvalidNestedObjectWithLogicalNodes() {
        final String invalidField = "address";
        final String filter = invalidField + "==4000 and firstname==William";
        Optional<ConstraintViolation<CustomAnnotationTestDelegate>> violation = validate(filter);
        assertTrue(violation.isPresent());
        assertTrue(validateConstraintViolationMessage(
                invalidNestedObjectResource,
                invalidNestedObjectMessage,
                violation.get(),
                invalidField,
                Person.class.getSimpleName()));
    }

    private boolean validateConstraintViolationMessage(String resourcePath,
                                                       String messagePath,
                                                       ConstraintViolation<CustomAnnotationTestDelegate> violation,
                                                       String... fields) {
        String message = ResourceBundle.getBundle(resourcePath).getString(messagePath);
        if (fields.length > 0) {
            return violation
                    .getMessage()
                    .equals(String.format(message, (Object[]) fields));
        }
        return violation
                .getMessage()
                .equals(message);
    }

    private Optional<ConstraintViolation<CustomAnnotationTestDelegate>> validate(String filter) {
        Method method;
        String[] parameterValues = {filter};
        try {
            method = CustomAnnotationTestDelegate.class.getMethod("validateFilterMethod", String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Could not find Method", e);
        }

        return executableValidator
                .validateParameters(testDelegate, method, parameterValues)
                .stream()
                .filter(v -> !v.getMessage().equals("{}"))
                .findFirst();
    }
}
