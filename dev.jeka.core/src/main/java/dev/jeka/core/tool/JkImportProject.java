package dev.jeka.core.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds a Jeka project def classpath to the Jeka project where this annotation is declared.
 * Typically, the annotated element is a a class from 'jeka/def' source directory.
 * When used on a public field of type  <code>JkCommands</code> the annotated field is
 * injected with the instance of the imported project.
 *
 * Examples : <code>@JkImportProject("../importedProject")</code>
 * a {@link JkCommands} field with value <code>../FooB</code>.
 * 
 * @author Jerome Angibaud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface JkImportProject {

    /** Dependee project relative path */
    String value();

}
