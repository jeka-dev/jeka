package dev.jeka.core.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the repositories to use for resolving dependencies declared with @{@link JkImport} elements.
 *
 * @author Jerome Angibaud
 */
@Target(ElementType.TYPE)
@Repeatable(JkImportRepo.ImportRepos.class)
@Retention(RetentionPolicy.SOURCE)
public @interface JkImportRepo {

    String value();

    @Target(ElementType.TYPE)
    @interface ImportRepos {
        JkImportRepo[] value();
    }
}
