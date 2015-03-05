package org.jake;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that the annotated element is a build of a another project.
 * The expected value is the referenced project path relative to the project
 * the annotated element belong. <br/>
 * So for example if there is A project FooA and and project FooB belonging to the
 * same folder. In project FooA we can reference project FooB by annotating
 * a {@link JakeBuild} field with value <code>../FooB</code>.
 * 
 * @author Jerome Angibaud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JakeProject {

	String value();

}
