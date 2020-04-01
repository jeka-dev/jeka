package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.file.JkResourceProcessor;
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
public class JkJavaProjectMakerCompilationStep<T> {

    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final JkJavaVersion DEFAULT_JAVA_VERSION = JkJavaVersion.V8;

    /**
     * For parent chaining
     */
    public final T __;

    private final JkJavaProjectMaker maker;

    private final JkRunnables<JkJavaProjectMakerCompilationStep<T>> beforeGenerate;

    private final JkRunnables<JkJavaProjectMakerCompilationStep<T>> afterGenerate;

    private final JkRunnables<JkJavaProjectMakerCompilationStep<T>> sourceGenerator;

    private final JkRunnables<JkJavaProjectMakerCompilationStep<T>> resourceGenerator;

    private final JkRunnables<JkJavaProjectMakerCompilationStep<T>> afterCompile;

    private final JkJavaCompiler<JkJavaProjectMakerCompilationStep<T>> compiler;

    private final JkResourceProcessor<JkJavaProjectMakerCompilationStep<T>> resourceProcessor;

    private final String scope;

    private final LinkedList<String> compileOptions = new LinkedList<>();

    private boolean done;

    private Supplier<JkJavaCompileSpec> compileSpecSupplier;

    private Supplier<JkPathTreeSet> resourceSupplier;

    private JkJavaVersion javaVersion;

    private String sourceEncoding;

    private JkJavaProjectMakerCompilationStep(JkJavaProjectMaker maker, String scope, T parent) {
        this.maker = maker;
        __ = parent;
        this.scope = scope;
        beforeGenerate = JkRunnables.noOp(this);
        afterGenerate = JkRunnables.noOp(this);
        sourceGenerator = JkRunnables.noOp(this);
        resourceGenerator = JkRunnables.noOp(this);
        afterCompile = JkRunnables.noOp(this);
        compiler = JkJavaCompiler.of(this);
        resourceProcessor = JkResourceProcessor.of(this);
    }


    static JkJavaProjectMakerCompilationStep<JkJavaProjectMaker> ofProd(JkJavaProjectMaker maker) {
        JkJavaProjectMakerCompilationStep result =
                new JkJavaProjectMakerCompilationStep(maker, "production code", maker.getSteps());
        result.compileSpecSupplier = () -> result.computeProdCompileSpec();
        result.resourceSupplier = () -> maker.project.getSourceLayout().getResources();
        return result;
    }

    static JkJavaProjectMakerCompilationStep<JkJavaProjectMakerTestingStep> ofTest(JkJavaProjectMaker maker,
                                                                        JkJavaProjectMakerTestingStep parent) {
        JkJavaProjectMakerCompilationStep result =
                new JkJavaProjectMakerCompilationStep(maker, "test code", parent);
        result.compileSpecSupplier = () -> result.computeTestCompileSpec(maker.getSteps().getCompilation());
        result.resourceSupplier = () -> maker.project.getSourceLayout().getTestResources();
        return result;
    }

    public JkJavaProjectMakerCompilationStep apply(Consumer<JkJavaProjectMakerCompilationStep> consumer) {
        consumer.accept(this);
        return this;
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
        JkLog.startTask("Compilation and resource processing of " + scope);
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
    public JkJavaCompiler<JkJavaProjectMakerCompilationStep<T>> getCompiler() {
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
    public JkJavaProjectMakerCompilationStep<T> setSourceEncoding(String sourceEncoding) {
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
    public JkJavaProjectMakerCompilationStep<T> addOptions(String ... options) {
        this.compileOptions.addAll(Arrays.asList(options));
        return this;
    }

    /**
     * Returns the resource processor.
     */
    public JkResourceProcessor<JkJavaProjectMakerCompilationStep<T>> getResourceProcessor() {
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
    public JkJavaProjectMakerCompilationStep<T> setJavaVersion(JkJavaVersion javaVersion) {
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
        Path dir = getComputedCompileSpec().getOutputDir();
        this.getResourceProcessor().generate(resourceSupplier.get(), dir);
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
                .setClasspath(maker.fetchDependenciesFor(JkJavaDepScopes.SCOPES_FOR_COMPILATION))
                .addSources(maker.project.getSourceLayout().getSources()
                        .and(maker.getOutLayout().getGeneratedSourceDir()))
                .addOptions(compileOptions)
                .setOutputDir(maker.getOutLayout().getClassDir());
    }

    private JkJavaCompileSpec computeTestCompileSpec(JkJavaProjectMakerCompilationStep prodStep) {
        JkJavaCompileSpec prodSpec = prodStep.getComputedCompileSpec();
        return JkJavaCompileSpec.of()
                .setSourceAndTargetVersion(javaVersion != null ? javaVersion : prodSpec.getSourceVersion())
                .setEncoding(sourceEncoding != null ? sourceEncoding : prodSpec.getEncoding())
                .setClasspath(maker.fetchDependenciesFor(JkJavaDepScopes.SCOPES_FOR_TEST)
                        .andPrepend(maker.getOutLayout().getClassDir()))
                .addSources(maker.project.getSourceLayout().getTests())
                .addOptions(compileOptions)
                .setOutputDir(maker.getOutLayout().getTestClassDir());
    }

}
