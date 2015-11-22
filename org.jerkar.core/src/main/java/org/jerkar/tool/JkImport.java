package org.jerkar.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the libraries necessary to compile and run the annotated class as a
 * Jerkar build. It can specify library referenced in a repository (as
 * "com.google.guava:guava:18.0") or file pattern relative to the project
 * directory (as "../lib-folder/mylib.jar").
 *
 * @author Jerome Angibaud
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface JkImport {

    /**
     * The dependency to import. It can be a module dependency (as "com.google.guava:guava:18.0", "../lib-folder.mylib.jar").
     */
    String[] value();

    /**
     * The repository where are located the dependencies, if it is a distinct repository than the defaults one.
     */
    String[] repo() default {};

}
