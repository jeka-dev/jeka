package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.file.JkResourceProcessor;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsObject;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Handles project compilation step. Users can configure inner phases by chaining runnables.
 * They also can modify {@link JkJavaCompiler} and {@link JkJavaCompileSpec} to use.
 */
public class JkJavaProjectCompilationStep<T> {

    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final JkJavaVersion DEFAULT_JAVA_VERSION = JkJavaVersion.V8;

    /**
     * For parent chaining
     */
    public final T __;

    private final JkJavaProject project;

    private final JkRunnables<JkJavaProjectCompilationStep<T>> beforeGenerate;

    private final JkRunnables<JkJavaProjectCompilationStep<T>> beforeCompile;

    private final JkConsumers<Path, JkJavaProjectCompilationStep<T>> sourceGenerator;

    private final JkConsumers<Path, JkJavaProjectCompilationStep<T>> resourceGenerator;

    private final JkRunnables<JkJavaProjectCompilationStep<T>> afterCompile;

    private final JkJavaCompiler<JkJavaProjectCompilationStep<T>> compiler;

    private final JkResourceProcessor<JkJavaProjectCompilationStep<T>> resourceProcessor;

    private final JkCompileLayout<JkJavaProjectCompilationStep<T>> layout;

    private final String scope;

    private final LinkedList<String> compileOptions = new LinkedList<>();

    private boolean done;

    private Supplier<JkJavaCompileSpec> compileSpecSupplier;

    private JkJavaVersion javaVersion;

    private String sourceEncoding;

    private JkJavaProjectCompilationStep(JkJavaProject project, String scope, T parent) {
        __ = parent;
        this.project = project;
        this.scope = scope;
        beforeGenerate = JkRunnables.noOp(this);
        sourceGenerator = JkConsumers.ofParent(this);
        resourceGenerator = JkConsumers.ofParent(this);
        beforeCompile = JkRunnables.noOp(this);
        compiler = JkJavaCompiler.ofParent(this);
        afterCompile = JkRunnables.noOp(this);
        resourceProcessor = JkResourceProcessor.ofParent(this);
        layout = JkCompileLayout.ofParent(this)
                .setBaseDirSupplier(project::getBaseDir)
                .setOutputDirSupplier(project::getOutputDir);
    }

    static JkJavaProjectCompilationStep<JkJavaProject.JkSteps> ofProd(JkJavaProject project, JkJavaProject.JkSteps parent) {
        JkJavaProjectCompilationStep result =
                new JkJavaProjectCompilationStep(project, "production code", parent);
        result.compileSpecSupplier = () -> result.computeProdCompileSpec();
        return result;
    }

    static JkJavaProjectCompilationStep<JkJavaProjectTestingStep> ofTest(JkJavaProject project,
                                                                         JkJavaProjectTestingStep parent) {
        JkJavaProjectCompilationStep result =
                new JkJavaProjectCompilationStep(project, "test code", parent);
        result.compileSpecSupplier = () -> result.computeTestCompileSpec(project.getSteps().getCompilation());
        result.layout.setSourceMavenStyle(JkCompileLayout.Concern.TEST);
        return result;
    }

    public JkJavaProjectCompilationStep apply(Consumer<JkJavaProjectCompilationStep> consumer) {
        consumer.accept(this);
        return this;
    }

    void reset() {
        done = false;
    }

    public JkCompileLayout<JkJavaProjectCompilationStep<T>> getLayout() {
        return layout;
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
        JkLog.startTask("Compilation and resource processing of " + scope);
        beforeGenerate.run();
        sourceGenerator.accept(this.layout.getGeneratedSourceDir());
        resourceGenerator.accept(this.layout.getGeneratedResourceDir());
        processResources();
        beforeCompile.run();
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
    public JkRunnables<JkJavaProjectCompilationStep<T>> getBeforeGenerate() {
        return beforeGenerate;
    }

    /**
     * Returns the runnables to run after source and resource generation. User can chain its own runnable
     * to customise the process. Empty by default.
     */
    public JkRunnables<JkJavaProjectCompilationStep<T>> getBeforeCompile() {
        return beforeCompile;
    }

    /**
     * Returns the consumers generating sources. User can chain its own consumer
     * to customise the process. Empty by default. The object passed as parameter of the consumers
     * is the base directory where sources must be generated.
     */
    public JkConsumers<Path, JkJavaProjectCompilationStep<T>> getSourceGenerator() {
        return sourceGenerator;
    }

    /**
     * Returns the consumers generating resources. User can chain its own consumer
     * to customise the process. Empty by default. The object passed as parameter of the consumers
     * is the base directory where resources must be generated.
     */
    public JkConsumers<Path, JkJavaProjectCompilationStep<T>> getResourceGenerator() {
        return resourceGenerator;
    }

    /**
     * Returns the compiler compiling Java sources of this project. The returned instance is mutable
     * so users can modify it from this method return.
     */
    public JkJavaCompiler<JkJavaProjectCompilationStep<T>> getCompiler() {
        return this.compiler;
    }

    /**
     * Returns the runnables to be run after compilation. User can chain its own runnable
     * to customise the process. Empty by default.
     */
    public JkRunnables getAfterCompile() {
        return afterCompile;
    }

    /**
     * Returns encoding to use to read Java source files
     */
    public String getSourceEncoding() {
        return getComputedCompileSpec().getEncoding();
    }

    /**
     * Set the encoding to use to read Java source files
     */
    public JkJavaProjectCompilationStep<T> setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
        return this;
    }

    /**
     * Returns extra compile options passed to the compiler
     */
    public List<String> getCompileOptions() {
        return Collections.unmodifiableList(compileOptions);
    }

    /**
     * Adds options to be passed to Java compiler
     */
    public JkJavaProjectCompilationStep<T> addOptions(String ... options) {
        this.compileOptions.addAll(Arrays.asList(options));
        return this;
    }

    /**
     * Returns the resource processor.
     */
    public JkResourceProcessor<JkJavaProjectCompilationStep<T>> getResourceProcessor() {
        return resourceProcessor;
    }

    /**
     * Gets the Java version used as source and target version
     */
    public JkJavaVersion getJavaVersion() {
        return getComputedCompileSpec().getSourceVersion();
    }

    /**
     * Sets the Java version used for both source and target.
     */
    public JkJavaProjectCompilationStep<T> setJavaVersion(JkJavaVersion javaVersion) {
        this.javaVersion = javaVersion;
        return this;
    }

    /**
     * Computes and returns the compile specification to pass to Java computer. The returned result is created
     * at each invokation and modify it has no side effect.
     */
    public JkJavaCompileSpec getComputedCompileSpec() {
        return compileSpecSupplier.get();
    }

    private void processResources() {
        this.getResourceProcessor().generate(layout.getResources(), layout.getClassDir());
    }

    private void runCompile() {
        boolean success = getCompiler().compile(compileSpecSupplier.get());
        if (!success) {
            throw new JkException("Compilation of Java sources failed.");
        }
    }

    private JkJavaCompileSpec computeProdCompileSpec() {
        return JkJavaCompileSpec.of()
                .setSourceAndTargetVersion(JkUtilsObject.firstNonNull(this.javaVersion, DEFAULT_JAVA_VERSION))
                .setEncoding(sourceEncoding != null ? sourceEncoding : DEFAULT_ENCODING)
                .setClasspath(project.getDependencyManagement()
                        .fetchDependencies(JkJavaDepScopes.SCOPES_FOR_COMPILATION).getFiles())
                .addSources(layout.getSources().and(layout.getGeneratedSourceDir()))
                .addOptions(compileOptions)
                .setOutputDir(layout.getClassDir());
    }

    private JkJavaCompileSpec computeTestCompileSpec(JkJavaProjectCompilationStep prodStep) {
        JkJavaCompileSpec prodSpec = prodStep.getComputedCompileSpec();
        return JkJavaCompileSpec.of()
                .setSourceAndTargetVersion(javaVersion != null ? javaVersion : prodSpec.getSourceVersion())
                .setEncoding(sourceEncoding != null ? sourceEncoding : prodSpec.getEncoding())
                .setClasspath(project.getDependencyManagement()
                        .fetchDependencies(JkJavaDepScopes.SCOPES_FOR_TEST).getFiles()
                            .andPrepend(prodStep.layout.getClassDir()))
                .addSources(layout.getSources().and(layout.getGeneratedSourceDir()))
                .addOptions(compileOptions)
                .setOutputDir(layout.getClassDir());
    }

}
