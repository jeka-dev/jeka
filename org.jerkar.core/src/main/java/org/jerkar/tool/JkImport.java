package org.jerkar.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
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
@Repeatable(JkImport.JkImports.class)
public @interface JkImport {

    /**
     * The dependency to import. It can be a module dependency (as "com.google.guava:guava:18.0")
     * or a file dependency (as "../lib-folder.mylib.jar").
     */
    String value();


    /**
     * Repeatable container.
     */
    @Target(ElementType.TYPE)
    @interface JkImports {
        JkImport[] value();
    }

}
