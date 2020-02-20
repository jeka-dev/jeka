package dev.jeka.core.tool;

import java.lang.annotation.*;

/**
 * Adds an element to def classpath. It can specify a library referenced in a repository (as
 * "com.google.guava:guava:18.0") or file pattern relative to the project
 * directory (as "../lib-folder/mylib.jar" or "libs/*.jar")
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
