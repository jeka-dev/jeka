package org.jerkar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the annotated field as an option, meaning that this field value can be set
 * in the command line while running Jake.
 * 
 * @author Jerome Angibaud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface JkOption {

	String[] value();

}
