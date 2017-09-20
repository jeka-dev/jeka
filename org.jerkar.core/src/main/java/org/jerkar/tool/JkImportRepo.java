package org.jerkar.tool;

import java.lang.annotation.*;

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
