package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkResourceProcessor;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Handles project compilation step. Users can configure inner phases by chaining runnables.
 * They also can modify {@link JkJavaCompiler} and {@link JkJavaCompileSpec} to use.
 */
public class JkJavaProjectCompilation<T> {

    private static final String PRODUCTION_PURPOSE = "production";

    private static final String TEST_PURPOSE = "test";

    /**
     * For parent chaining
     */
    public final T __;

    private final JkJavaProjectConstruction construction;

    private final JkRunnables<JkJavaProjectCompilation<T>> preGenerateActions;

    private final JkRunnables<JkJavaProjectCompilation<T>> preCompileActions;

    private final JkConsumers<Path, JkJavaProjectCompilation<T>> sourceGenerator;

    private final JkConsumers<Path, JkJavaProjectCompilation<T>> resourceGenerator;

    private final JkRunnables<JkJavaProjectCompilation<T>> postCompileActions;

    private final JkResourceProcessor<JkJavaProjectCompilation<T>> resourceProcessor;

    private final JkCompileLayout<JkJavaProjectCompilation<T>> layout;

    private Function<JkDependencySet, JkDependencySet> dependenciesModifier = deps -> deps;

    private Supplier<JkDependencySet> dependencyBootSupplier = () -> JkDependencySet.of();

    private final LinkedList<String> compileOptions = new LinkedList<>();

    private boolean done;

    private String purpose;

    private Supplier<JkJavaCompileSpec> compileSpecSupplier;

    private JkJavaProjectCompilation(JkJavaProjectConstruction construction, String purpose, T parent) {
        __ = parent;
        this.purpose = purpose;
        this.construction = construction;
        preGenerateActions = JkRunnables.ofParent(this);
        sourceGenerator = JkConsumers.ofParent(this);
        resourceGenerator = JkConsumers.ofParent(this);
        preCompileActions = JkRunnables.ofParent(this);
        postCompileActions = JkRunnables.ofParent(this);
        resourceProcessor = JkResourceProcessor.ofParent(this);
        layout = JkCompileLayout.ofParent(this)
                .setBaseDirSupplier(construction.getProject()::getBaseDir)
                .setOutputDirSupplier(construction.getProject()::getOutputDir);
    }

    static JkJavaProjectCompilation<JkJavaProjectConstruction> ofProd(JkJavaProjectConstruction projectProduction) {
        JkJavaProjectCompilation result =
                new JkJavaProjectCompilation(projectProduction, PRODUCTION_PURPOSE, projectProduction);
        result.compileSpecSupplier = () -> result.computeProdCompileSpec();
        return result;
    }

    static JkJavaProjectCompilation<JkJavaProjectTesting> ofTest(JkJavaProjectConstruction construction,
                                                                 JkJavaProjectTesting parent) {
        JkJavaProjectCompilation result =
                new JkJavaProjectCompilation(construction, TEST_PURPOSE, parent);
        result.dependencyBootSupplier = () -> construction.getRuntimeDependencies().merge(construction
            .getCompilation().getDependencies()).getResult();
        result.compileSpecSupplier = () -> result.computeTestCompileSpec(construction.getCompilation());
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
        JkLog.startTask("Make " + purpose);
        preGenerateActions.run();
        sourceGenerator.accept(this.layout.resolveGeneratedSourceDir());
        resourceGenerator.accept(this.layout.resolveGeneratedResourceDir());
        processResources();
        preCompileActions.run();
        runCompile();
        postCompileActions.run();
        JkLog.endTask();
    }

    /**
     * As #run but perform only if not already done.
     */
    public void runIfNecessary() {
        if (done) {
            JkLog.trace("Compilation task already done. Won't perform again.");
        } else {
            run();
            done = true;
        }
    }

    /**
     * Returns the runnables to run prior source and resource generation. User can chain its own runnable
     * to customise the process. Empty by default.
     */
    public JkRunnables<JkJavaProjectCompilation<T>> getPreGenerateActions() {
        return preGenerateActions;
    }

    /**
     * Returns the runnables to run after source and resource generation. User can chain its own runnable
     * to customise the process. Empty by default.
     */
    public JkRunnables<JkJavaProjectCompilation<T>> getPreCompileActions() {
        return preCompileActions;
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
     * Returns the runnables to be run after compilation. User can chain its own runnable
     * to customise the process. Empty by default.
     */
    public JkRunnables getPostCompileActions() {
        return postCompileActions;
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

    public JkJavaProjectCompilation<T> setDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        this.dependenciesModifier = dependenciesModifier.andThen(modifier);
        return this;
    }

    public JkResolveResult resolveDependencies() {
        return construction.getDependencyResolver().resolve(getDependencies());
    }

    public JkDependencySet getDependencies() {
        return dependenciesModifier.apply(dependencyBootSupplier.get());
    }

    private void processResources() {
        this.getResourceProcessor().generate(layout.resolveResources(), layout.resolveClassDir());
    }

    private void runCompile() {
        boolean success = construction.getCompiler().compile(compileSpecSupplier.get());
        if (!success) {
            throw new IllegalStateException("Compilation of Java sources failed.");
        }
    }

    private JkJavaCompileSpec computeProdCompileSpec() {
        return JkJavaCompileSpec.of()
            .setSourceAndTargetVersion(construction.getJavaVersion())
            .setEncoding(construction.getSourceEncoding())
            .setClasspath(resolveDependencies().getFiles())
            .addSources(layout.resolveSources().and(JkPathTree.of(layout.resolveGeneratedSourceDir())))
            .addOptions(compileOptions)
            .setOutputDir(layout.resolveClassDir());
    }

    private JkJavaCompileSpec computeTestCompileSpec(JkJavaProjectCompilation prodStep) {
        JkDependencySet dependencies = getDependencies();
        return JkJavaCompileSpec.of()
                .setSourceAndTargetVersion(construction.getJavaVersion())
                .setEncoding(construction.getSourceEncoding())
                .setClasspath(construction.getDependencyResolver().resolve(dependencies).getFiles()
                            .andPrepend(prodStep.layout.resolveClassDir()))
                .addSources(layout.resolveSources().and(layout.resolveGeneratedSourceDir()))
                .addOptions(compileOptions)
                .setOutputDir(layout.resolveClassDir());
    }

}
