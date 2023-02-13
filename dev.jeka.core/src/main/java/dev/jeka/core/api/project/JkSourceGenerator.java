package dev.jeka.core.api.project;

import java.nio.file.Path;

public abstract class JkSourceGenerator {

    protected JkSourceGenerator() {
    }

    /**
     * Sources will be generated under the <i>output/generated_sources/[name returned by thus method]</i>
     * This path will be passed as argument to {@link #generate(JkProject, Path)}
     */
    protected abstract String getDirName();

    /**
     * Generates source code under the supplied source directory.
     * 
     * @param project The project for which the sources will be generated.
     * @param generatedSourceDir The dir where the source should be generated
     *                
     */
    protected abstract void generate(JkProject project, Path generatedSourceDir);

}
