package dev.jeka.core.api.java.project;

import dev.jeka.core.api.java.JkJavaCompileSpec;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.file.JkResourceProcessor;
import dev.jeka.core.api.system.JkLog;

import java.nio.charset.Charset;

public class JkJavaProjectCompileTasks {

    private final JkJavaProjectMaker maker;

    private final JkRunnables preCompile = JkRunnables.noOp();

    private final JkRunnables sourceGenerator = JkRunnables.noOp();

    private final JkRunnables resourceGenerator = JkRunnables.noOp();

    private final JkRunnables postActions = JkRunnables.noOp();

    private final JkRunnables resourceProcessor;

    private final JkRunnables compileRunner;

    private JkJavaCompiler compiler = JkJavaCompiler.ofJdk();

    private boolean done;

    JkJavaProjectCompileTasks(JkJavaProjectMaker maker, Charset charset) {
        this.maker = maker;
        resourceProcessor = JkRunnables.of(() -> JkResourceProcessor.of(maker.project.getSourceLayout().getResources())
                .and(maker.getOutLayout().getGeneratedResourceDir())
                .and(maker.project.getResourceInterpolators())
                .generateTo(maker.getOutLayout().getClassDir(), charset));
        compileRunner = JkRunnables.of(() -> {
            final JkJavaCompileSpec compileSpec = compileSourceSpec();
            compiler.compile(compileSpec);
        });
    }

    void reset() {
        done = false;
    }

    /**
     * Performs entire compilation phase, including : <ul>
     * <li>Generating resources</li>
     * <li>Generating sources</li>
     * <li>Processing resources (interpolation)</li>
     * <li>Compiling sources</li>
     * </ul>
     */
    public void run() {
        JkLog.startTask("Compilation and resource processing");
        preCompile.run();
        sourceGenerator.run();
        resourceGenerator.run();
        compileRunner.run();
        resourceProcessor.run();
        postActions.run();
        JkLog.endTask();
    }

    /**
     * As #run but perform only if not already done.
     */
    public void runIfNecessary() {
        if (done) {
            JkLog.trace("Compilation task already done. Won't perfom again.");
        } else {
            run();
            done = true;
        }
    }

    public JkRunnables getPreCompile() {
        return preCompile;
    }

    public JkRunnables getSourceGenerator() {
        return sourceGenerator;
    }

    public JkRunnables getResourceGenerator() {
        return resourceGenerator;
    }

    public JkRunnables getPostActions() {
        return postActions;
    }

    public JkRunnables getResourceProcessor() {
        return resourceProcessor;
    }

    public JkRunnables getCompileRunner() {
        return compileRunner;
    }

    public JkJavaCompiler getCompiler() {
        return compiler;
    }

    public JkJavaProjectCompileTasks setCompiler(JkJavaCompiler compiler) {
        this.compiler = compiler;
        return this;
    }

    public JkJavaProjectCompileTasks setFork(boolean fork, String ... params) {
        this.compiler = this.compiler.withForking(fork, params);
        return this;
    }

    private JkJavaCompileSpec compileSourceSpec() {
        JkJavaCompileSpec result = maker.project.getCompileSpec().copy();
        final JkPathSequence classpath = maker.fetchDependenciesFor(JkJavaDepScopes.SCOPES_FOR_COMPILATION);
        return result
                .setClasspath(classpath)
                .addSources(maker.project.getSourceLayout().getSources())
                .addSources(maker.getOutLayout().getGeneratedSourceDir())
                .setOutputDir(maker.getOutLayout().getClassDir());
    }
}
