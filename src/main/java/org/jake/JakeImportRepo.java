package org.jake;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the repositories (Maven or Ivy) where referenced libraries in {@link JakeImport}
 * are supposed to be found.
 * 
 * @author Jérôme Angibaud
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface JakeImportRepo {

	String[] value();

}
