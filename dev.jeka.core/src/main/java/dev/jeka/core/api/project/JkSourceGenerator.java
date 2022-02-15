package dev.jeka.core.api.project;

import java.nio.file.Path;

public interface JkSourceGenerator {

    /**
     * Sources will be generated under the <i>output/generated_sources/[name returned by thus method]</i>
     */
    String getDirName();

    /**
     * Generates source code under the supplied source directory.
     */
    void generate(Path sourceDir);
}
