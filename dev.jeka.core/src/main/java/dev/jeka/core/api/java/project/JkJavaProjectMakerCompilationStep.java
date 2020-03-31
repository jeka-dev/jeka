package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkResourceProcessor;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.system.JkLog;

/**
 * Handles project compilation step. Users can configure inner phases by chaining runnables.
 * They also can modify {@link JkJavaCompiler} and {@link JkJavaCompileSpec} to use.
 */
public abstract class JkJavaProjectMakerCompilationStep<T extends JkJavaProjectMakerCompilationStep> {

    private final JkRunnables beforeGenerate = JkRunnables.noOp();

    private final JkRunnables afterGenerate = JkRunnables.noOp();

    private final JkRunnables sourceGenerator = JkRunnables.noOp();

    private final JkRunnables resourceGenerator = JkRunnables.noOp();

    private final JkRunnables afterCompile = JkRunnables.noOp();

    private boolean done;

    private JkJavaCompiler<T> compiler;

    private final JkJavaProjectMaker maker;

    protected JkJavaProjectMakerCompilationStep(JkJavaProjectMaker maker) {
        this.maker = maker;
    }

    protected void init() {
        this.compiler = (JkJavaCompiler<T>) JkJavaCompiler.of(this);
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
        JkLog.startTask("Compilation and resource processing of " + getScope());
        afterGenerate.run();
        sourceGenerator.run();
        resourceGenerator.run();
        afterGenerate.run();
        processResources();
        runCompile();
        afterCompile.run();
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

    /**
     * Returns the runnables to run prior source and resource generation. User can chain its own runnable
     * to customise the process. Empty by default.
     */
    public JkRunnables getBeforeGenerate() {
        return beforeGenerate;
    }

    /**
     * Returns the runnables to run after source and resource generation. User can chain its own runnable
     * to customise the process. Empty by default.
     */
    public JkRunnables getAfterGenerate() {
        return afterGenerate;
    }

    /**
     * Returns the runnables generating sources. User can chain its own runnable
     * to customise the process. Empty by default.
     */
    public JkRunnables getSourceGenerator() {
        return sourceGenerator;
    }

    /**
     * Returns the runnables generating resources. User can chain its own runnable
     * to customise the process. Empty by default.
     */
    public JkRunnables getResourceGenerator() {
        return resourceGenerator;
    }

    /**
     * Returns the compiler compiling Java sources of this project. The returned instance is mutable
     * so users can modify it from this method return.
     */
    public JkJavaCompiler<T> getCompiler() {
        return this.compiler;
    }

    /**
     * Returns the runnables to be run after compilation. User can chain its own runnable
     * to customise the process. Empty by default.
     */
    public JkRunnables getAfterCompile() {
        return afterCompile;
    }

    protected abstract JkJavaCompileSpec<T> getCompileSpec();

    protected abstract JkResourceProcessor<T> getResourceProcessor();


    protected abstract String getScope();

    private void processResources() {
        this.getResourceProcessor().generateTo(maker.getOutLayout().getClassDir());
    }

    private void runCompile() {
        boolean success = getCompiler().compile(getCompileSpec());
        if (!success) {
            throw new JkException("Compilation of Java sources failed.");
        }
    }

    public static class JkProduction extends JkJavaProjectMakerCompilationStep<JkProduction> {

        private JkJavaCompileSpec<JkProduction> compileSpec;

        private JkResourceProcessor<JkProduction> resourceProcessor;

        /**
         * For parent chaining
         */
        public final JkJavaProjectMaker.JkSteps _;

        private JkProduction(JkJavaProjectMaker maker) {
            super(maker);
            _ = maker.getSteps();
        }

        @Override
        protected void init() {
            super.init();
            resourceProcessor = JkResourceProcessor.of(this)
                    .setResources(super.maker.project.getSourceLayout().getResources())
                    .addResources(super.maker.getOutLayout().getGeneratedResourceDir());
            compileSpec = defaultCompileSourceSpec(this, super.maker);
        }

        static JkProduction of(JkJavaProjectMaker maker) {
            JkProduction result = new JkProduction(maker);
            result.init();
            return result;
        }

        /**
         * Returns classes to compile. The returned instance is mutable
         * so users can modify it from this method return.
         */
        public JkJavaCompileSpec<JkProduction> getCompileSpec() {
            return compileSpec;
        }

        /**
         * Returns the object that will process the resources (copying, interpolating).
         * The returned instance is mutable so users can modify it from this method return.
         */
        public JkResourceProcessor getResourceProcessor() {
            return resourceProcessor;
        }

        @Override
        protected String getScope() {
            return "production code";
        }

        private static JkJavaCompileSpec defaultCompileSourceSpec(JkProduction productionCompileStep,
                                                                  JkJavaProjectMaker maker) {
            final JkPathSequence classpath = maker.fetchDependenciesFor(JkJavaDepScopes.SCOPES_FOR_COMPILATION);
            return JkJavaCompileSpec.of(productionCompileStep)
                    .setSourceAndTargetVersion(JkJavaVersion.V8)
                    .setEncoding("UTF-8")
                    .setClasspath(classpath)
                    .addSources(maker.project.getSourceLayout().getSources())
                    .addSources(maker.getOutLayout().getGeneratedSourceDir())
                    .setOutputDir(maker.getOutLayout().getClassDir());
        }


    }

}
