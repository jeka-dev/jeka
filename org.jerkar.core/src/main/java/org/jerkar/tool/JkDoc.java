package org.jerkar.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents the annotated element so that Jerkar can display some information
 * when 'help' or 'helpPlugins' are invoked from command line.
 *
 * @author Jerome Angibaud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.FIELD })
@Inherited
public @interface JkDoc {

    /**
     * The text to display when help is requested.
     */
    String[] value();

}
