package dev.jeka.core.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that the annotated element is a JkCommands of a another project. This
 * annotated element is supposed to be a {@link JkCommands} or instance of a subclass. The
 * expected value is the referenced project path relative to the project the
 * annotated element belong. <br/>
 * So for example if there is a project FooA and andPrepending a project FooB belonging to
 * the same folder. In project FooA we can reference project FooB by annotating
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
