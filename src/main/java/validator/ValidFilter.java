package validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FilterValidator.class)
@Component
public @interface ValidFilter {

    String message() default "{}";

    Class<?> clazz() ;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}