package org.jerkar.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the libraries necessary to compile and run the annotated class as a Jerkar build.
 * It can specify library referenced in a repository (as "com.google.guava:guava:18.0")
 * or files relative to the project directory (as "../libs/mylib.jar").
 * 
 * @author Jérôme Angibaud
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface JkImport {

	String[] value();

	String[] repos() default "";

}
