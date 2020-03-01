package dev.jeka.core.tool;

import java.lang.annotation.*;

/**
 * Specifies the repositories to use for resolving dependencies declared with @{@link JkDefClasspath} elements.
 *
 * @author Jerome Angibaud
 */
@Target(ElementType.TYPE)
@Repeatable(JkDefRepo.ImportRepos.class)
@Retention(RetentionPolicy.SOURCE)
public @interface JkDefRepo {

    String value();

    @Target(ElementType.TYPE)
    @interface ImportRepos {
        JkDefRepo[] value();
    }
}
