package validator;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.AbstractNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.Node;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.lang.reflect.Field;
import java.util.*;

@Slf4j
public class FilterValidator implements ConstraintValidator<ValidFilter, String> {

    private Class<?> clazz;
    private ConstraintValidatorContext context;

    @Override
    public void initialize(ValidFilter constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        this.clazz = constraintAnnotation.clazz();
    }

    @Override
    public boolean isValid(String filter, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(filter)) {
            return true;
        }
        this.context = context;

        AbstractNode node;
        try {
            node = (AbstractNode) new RSQLParser().parse(filter);
        } catch (Exception e) {
            log.error("The filter is invalid", e);
            context
                    .buildConstraintViolationWithTemplate("{ethias.oi.rxjs.ValidFilter.invalidSyntax}")
                    .addConstraintViolation();
            return false;
        }
        return parseNode(node);
    }

    /**
     * Recursive Method to parse the root node of the filter. This node is of type ComparisonNode if it's a simple comparison (== or gt for ex.)
     * and LogicalNode for a more complex filter ( x==y and a==b or c>d).
     * The method is recursively called in case of a LogicalNode which contains one or more ComparisonNode.
     * @param node the node to parse which represents the filter
     */
    private boolean parseNode(Node node) {
        List<Field> fields = parseClass(clazz);
        if (node instanceof ComparisonNode) {
            String[] selectors = ((ComparisonNode) node).getSelector().split("\\.");
            return compareFields(fields, selectors);

        } else if (node instanceof LogicalNode n) {
            return n.getChildren()
                    .stream()
                    .allMatch(this::parseNode);
        }
        return true;
    }

    /**
     * Method to compare the field of the ComparisonNode with the fields of the entity to which the filter is applied to.
     * In case of no fields in the entity are matching with the filter an error message is send.
     * In case of a nested entity inside the root entity, the nestedEntity is identified by the annotation @nestedObject.
     *
     * @param fields    An array list of all the fields contained in the entity and in its super class
     * @param selectors An array of String. The first element of the array is the name of the field to which the filter is applied to.
     *                  In case of nested entity, the second element of the array is the name of the field of the nested entity to which the filter is applied.
     * @return  true if the field in the filter corresponds to a field in the entity. False if no fields corresponds in the entity
     */
    private boolean compareFields(List<Field> fields, String[] selectors) {
        Optional<Field> optional = fields
                .stream()
                .filter(f -> f.getName().equals(selectors[0]))
                .findFirst();

        if (optional.isEmpty()) {
            sendInvalidFieldMessage(selectors[0]);
            return false;
        }
        Field field = optional.get();
        if (field.getAnnotation(NestedObject.class) != null) {
            return compareNestedObjects(field, selectors);
        }
        return true;
    }

    /**
     * Method  which contains the logic about the comparison of the fields of a nested entity with the field contained in the filter
     *
     * @param field     The nested entity to compare to the filter field.
     * @param selectors An array of String. The first element of the array is the name of the field to which the filter is applied to.
     *                  In case of nested entity, the second element of the array is the name of the field of the nested entity to which the filter is applied.
     * @return true if the field in the filter corresponds to a field in the nested entity false if the field in the filter doesn't correspond to a field in the nested entity or if there is no field
     */
    private boolean compareNestedObjects(Field field, String[] selectors) {
        if (selectors.length < 2) {
            sendInvalidNestedObjectMessage(selectors[0], field.getDeclaringClass().getSimpleName());
            return false;
        }
        List<Field> nestedFields = parseClass(field.getType());
        return compareFields(nestedFields, new String[]{selectors[1]});
    }

    /**
     * Method to retrieve all the fields contained in an entity and in its superclass
     * @param clazz  The class to parse
     * @return the list of fields contained in the entity
     */
    private List<Field> parseClass(Class<?> clazz) {
        List<Field> fields = new ArrayList<>(List.of(clazz.getDeclaredFields()));
        if (clazz.getSuperclass() != null) {
            fields.addAll(List.of(clazz.getSuperclass().getDeclaredFields()));
        }
        return fields;
    }

    /**
     * Method to send a ConstraintViolationMessage  in case of the filter mentioning a nested object is incorrect.
     * @param selector  The selector mentioned in the field
     * @param  className The name of the class to which the filter is applied to.
     */
    private void sendInvalidNestedObjectMessage(String selector, String className) {
        log.error("The filter is invalid: please add the name of the field you are searching for in nested object <" + selector + "> in class " + className);

        String message = ResourceBundle
                .getBundle("ValidationMessages")
                .getString("ethias.oi.rxjs.ValidFilter.invalidNestedObject");

        context
                .buildConstraintViolationWithTemplate(String.format(message, selector, className))
                .addConstraintViolation();
    }

    /**
     * Method to send a ConstraintViolationMessage  in case of the fitler mentioning a field is incorrect.
     * @param selector  The selector mentioned in the field
     */
    private void sendInvalidFieldMessage(String selector) {
        log.error("The filter is invalid: the field <" + selector + "> can't be found in " + clazz.getSimpleName() + ".");

        String message = ResourceBundle
                .getBundle("ValidationMessages")
                .getString("ethias.oi.rxjs.ValidFilter.unknownField");

        context
                .buildConstraintViolationWithTemplate(String.format(message, selector, clazz.getSimpleName()))
                .addConstraintViolation();
    }
}
