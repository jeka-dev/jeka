package org.jerkar.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that the annotated element is a build of a another project. This
 * annotated element is supposed to be a {@link JkBuild} or its subclasses. The
 * expected value is the referenced project path relative to the project the
 * annotated element belong. <br/>
 * So for example if there is a project FooA and and a project FooB belonging to
 * the same folder. In project FooA we can reference project FooB by annotating
 * a {@link JkBuild} field with value <code>../FooB</code>.
 * 
 * @author Jerome Angibaud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JkImportBuild {

    /** Dependee project relative path */
    String value();

}
