package dev.jeka.core.api.depmanagement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Tag to let IDE recognise that the value can be suggested with dependency coordinate
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
public @interface JkDepSuggest {

    /**
     * We can specify a query to narrow the search, as "com.google.guava:guava-".
     */
    String hint() default "";

    /**
     * Mention that we are interested in retrieving versions only.
     * If true, this has to be used in conjunction of {@link #hint()} by specifying a complete
     * group and name as *com.google.guava:guava:*
     */
    boolean versionOnly() default false;
}
