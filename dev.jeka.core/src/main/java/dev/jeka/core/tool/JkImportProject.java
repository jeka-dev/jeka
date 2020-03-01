package dev.jeka.core.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds a Jeka project def classpath to the Jeka project where this annotation is declared.
 * Typically, the annotated element is a <code>JkCommandSet</code> class from 'jeka/def' source directory.
 * But when used on a public field of type  <code>JkCommandSet</code> within a <code>JkCommandSet</code> class,
 * the annotated field is injected with an initialised <code>JkCommandSet</code> instance of the imported project.
 *
 * Examples : <code>@JkImportProject("../importedProject")</code>
 * a {@link JkCommandSet} field with value <code>../FooB</code>.
 * 
 * @author Jerome Angibaud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface JkImportProject {

    /** Dependee project relative path */
    String value();

}
