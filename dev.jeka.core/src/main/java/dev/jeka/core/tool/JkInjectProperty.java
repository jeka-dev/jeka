package dev.jeka.core.tool;

import java.lang.annotation.*;

/**
 * Injects the environment variable value if such a one exists and an option as not been already injected on.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@Inherited
public @interface JkInjectProperty {

    String value();

}
