package dev.jeka.core.api.project;

import java.nio.file.Path;

public abstract class JkSourceGenerator {

    private final JkProject project;

    protected JkSourceGenerator(JkProject project) {
        this.project = project;
    }

    /**
     * Sources will be generated under the <i>output/generated_sources/[name returned by thus method]</i>
     */
    public abstract String getDirName();

    /**
     * Generates source code under the supplied source directory.
     */
    public abstract void generate(Path sourceDir);

    protected JkProject getProject() {
        return project;
    }

    public JkSourceGenerator bindToProd() {
        project.getCompilation().addSourceGenerator(this);
        return this;
    }

    public JkSourceGenerator bindToTest() {
        project.getTesting().getCompilation().addSourceGenerator(this);
        return this;
    }
}
