package dev.jeka.core.tool;

import java.lang.annotation.*;

/**
 * Documents the annotated element so that Jeka can display some information
 * when 'help' is invoked from command line.
 *
 * @author Jerome Angibaud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.FIELD })
@Inherited
public @interface JkDoc {

    /**
     * The text to display when help is requested. The first line is for short description (or heading).
     */
    String value() default "";

    /**
     * If true, the method/field won't be displayed in 'help' command.
     */
    boolean hide() default false;

}
