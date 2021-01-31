package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.file.JkResourceProcessor;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.JkJavaVersion;
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
public class JkJavaProjectCompilation<T> {

    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final JkJavaVersion DEFAULT_JAVA_VERSION = JkJavaVersion.V8;

    /**
     * For parent chaining
     */
    public final T __;

    private final JkJavaProjectConstruction projectProduction;

    private final JkRunnables<JkJavaProjectCompilation<T>> beforeGenerate;

    private final JkRunnables<JkJavaProjectCompilation<T>> beforeCompile;

    private final JkConsumers<Path, JkJavaProjectCompilation<T>> sourceGenerator;

    private final JkConsumers<Path, JkJavaProjectCompilation<T>> resourceGenerator;

    private final JkRunnables<JkJavaProjectCompilation<T>> afterCompile;

    private final JkJavaCompiler<JkJavaProjectCompilation<T>> compiler;

    private final JkResourceProcessor<JkJavaProjectCompilation<T>> resourceProcessor;

    private final JkCompileLayout<JkJavaProjectCompilation<T>> layout;

    private JkDependencySet dependencySet = JkDependencySet.of();

    private final LinkedList<String> compileOptions = new LinkedList<>();

    private boolean done;

    private String purpose;

    private Supplier<JkJavaCompileSpec> compileSpecSupplier;

    private JkJavaVersion javaVersion = DEFAULT_JAVA_VERSION;

    private String sourceEncoding = DEFAULT_ENCODING;

    private JkJavaProjectCompilation(JkJavaProjectConstruction projectProduction, String purpose, T parent) {
        __ = parent;
        this.purpose = purpose;
        this.projectProduction = projectProduction;
        beforeGenerate = JkRunnables.ofParent(this);
        sourceGenerator = JkConsumers.ofParent(this);
        resourceGenerator = JkConsumers.ofParent(this);
        beforeCompile = JkRunnables.ofParent(this);
        compiler = JkJavaCompiler.ofParent(this);
        afterCompile = JkRunnables.ofParent(this);
        resourceProcessor = JkResourceProcessor.ofParent(this);
        layout = JkCompileLayout.ofParent(this)
                .setBaseDirSupplier(projectProduction.getProject()::getBaseDir)
                .setOutputDirSupplier(projectProduction.getProject()::getOutputDir);
    }

    static JkJavaProjectCompilation<JkJavaProjectConstruction> ofProd(JkJavaProjectConstruction projectProduction) {
        JkJavaProjectCompilation result =
                new JkJavaProjectCompilation(projectProduction, "production code", projectProduction);
        result.compileSpecSupplier = () -> result.computeProdCompileSpec();
        return result;
    }

    static JkJavaProjectCompilation<JkJavaProjectTesting> ofTest(JkJavaProjectConstruction projectProduction,
                                                                 JkJavaProjectTesting parent) {
        JkJavaProjectCompilation result =
                new JkJavaProjectCompilation(projectProduction, "test code", parent);
        result.compileSpecSupplier = () -> result.computeTestCompileSpec(projectProduction.getCompilation());
        result.layout
                .setSourceMavenStyle(JkCompileLayout.Concern.TEST)
                .setStandardOuputDirs(JkCompileLayout.Concern.TEST);
        return result;
    }

    public JkJavaProjectCompilation apply(Consumer<JkJavaProjectCompilation> consumer) {
        consumer.accept(this);
        return this;
    }

    public JkCompileLayout<JkJavaProjectCompilation<T>> getLayout() {
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
        JkLog.startTask("Compile and process resources for '" + purpose + "'");
        beforeGenerate.run();
        sourceGenerator.accept(this.layout.resolveGeneratedSourceDir());
        resourceGenerator.accept(this.layout.resolveGeneratedResourceDir());
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
    public JkRunnables<JkJavaProjectCompilation<T>> getBeforeGenerate() {
        return beforeGenerate;
    }

    /**
     * Returns the runnables to run after source and resource generation. User can chain its own runnable
     * to customise the process. Empty by default.
     */
    public JkRunnables<JkJavaProjectCompilation<T>> getBeforeCompile() {
        return beforeCompile;
    }

    /**
     * Returns the consumers generating sources. User can chain its own consumer
     * to customise the process. Empty by default. The object passed as parameter of the consumers
     * is the base directory where sources must be generated.
     */
    public JkConsumers<Path, JkJavaProjectCompilation<T>> getSourceGenerator() {
        return sourceGenerator;
    }

    /**
     * Returns the consumers generating resources. User can chain its own consumer
     * to customise the process. Empty by default. The object passed as parameter of the consumers
     * is the base directory where resources must be generated.
     */
    public JkConsumers<Path, JkJavaProjectCompilation<T>> getResourceGenerator() {
        return resourceGenerator;
    }

    /**
     * Returns the compiler compiling Java sources of this project. The returned instance is mutable
     * so users can modify it from this method return.
     */
    public JkJavaCompiler<JkJavaProjectCompilation<T>> getCompiler() {
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
        return sourceEncoding;
    }

    /**
     * Set the encoding to use to read Java source files
     */
    public JkJavaProjectCompilation<T> setSourceEncoding(String sourceEncoding) {
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
    public JkJavaProjectCompilation<T> addOptions(String ... options) {
        this.compileOptions.addAll(Arrays.asList(options));
        return this;
    }

    /**
     * Returns the resource processor.
     */
    public JkResourceProcessor<JkJavaProjectCompilation<T>> getResourceProcessor() {
        return resourceProcessor;
    }

    /**
     * Gets the Java version used as source and target version
     */
    public JkJavaVersion getJavaVersion() {
        return javaVersion;
    }

    /**
     * Sets the Java version used for both source and target.
     */
    public JkJavaProjectCompilation<T> setJavaVersion(JkJavaVersion javaVersion) {
        this.javaVersion = javaVersion;
        return this;
    }

    /**
     * Sets the Java version used for both source and target.
     */
    public JkJavaProjectCompilation<T> addDependencies(JkDependencySet dependencySet) {
        this.dependencySet = dependencySet.and(dependencySet);
        return this;
    }

    public JkDependencySet getDependencies() {
        return dependencySet;
    }

    private JkJavaCompileSpec getComputedCompileSpec() {
        return compileSpecSupplier.get();
    }

    private void processResources() {
        this.getResourceProcessor().generate(layout.resolveResources(), layout.resolveClassDir());
    }

    private void runCompile() {
        boolean success = getCompiler().compile(compileSpecSupplier.get());
        if (!success) {
            throw new IllegalStateException("Compilation of Java sources failed.");
        }
    }

    private JkJavaCompileSpec computeProdCompileSpec() {
        return JkJavaCompileSpec.of()
            .setSourceAndTargetVersion(JkUtilsObject.firstNonNull(this.javaVersion, DEFAULT_JAVA_VERSION))
            .setEncoding(sourceEncoding != null ? sourceEncoding : DEFAULT_ENCODING)
            .setClasspath(projectProduction.getDependencyResolver().resolve(dependencySet).getFiles())
            .addSources(layout.resolveSources().and(layout.resolveGeneratedSourceDir()))
            .addOptions(compileOptions)
            .setOutputDir(layout.resolveClassDir());
    }

    private JkJavaCompileSpec computeTestCompileSpec(JkJavaProjectCompilation prodStep) {
        JkJavaCompileSpec prodSpec = prodStep.getComputedCompileSpec();
        return JkJavaCompileSpec.of()
                .setSourceAndTargetVersion(javaVersion != null ? javaVersion : prodSpec.getSourceVersion())
                .setEncoding(sourceEncoding != null ? sourceEncoding : prodSpec.getEncoding())
                .setClasspath(projectProduction.getDependencyResolver().resolve(dependencySet).getFiles()
                            .andPrepend(prodStep.layout.resolveClassDir()))
                .addSources(layout.resolveSources().and(layout.resolveGeneratedSourceDir()))
                .addOptions(compileOptions)
                .setOutputDir(layout.resolveClassDir());
    }

}
