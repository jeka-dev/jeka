package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkResourceProcessor;
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
import java.util.stream.Collectors;

/**
 * Handles project compilation step. Users can configure inner phases by chaining runnables.
 * They also can modify {@link JkJavaCompiler} and {@link JkJavaCompileSpec} to use.
 */
public class JkProjectCompilation<T> {

    public static final String RESOURCES_PROCESS_ACTION = "resources-process";

    public static final String JAVA_SOURCES_COMPILE_ACTION = "java-sources-compile";

    private static final String PRODUCTION_PURPOSE = "production";

    private static final String TEST_PURPOSE = "test";

    /**
     * For parent chaining
     */
    public final T __;

    private final JkProjectConstruction construction;

    private final JkRunnables<JkProjectCompilation<T>> preCompileActions;

    private final JkRunnables<JkProjectCompilation<T>> compileActions;

    private final JkRunnables<JkProjectCompilation<T>> postCompileActions;

    private final JkResourceProcessor<JkProjectCompilation<T>> resourceProcessor;

    private final JkCompileLayout<JkProjectCompilation<T>> layout;

    private Function<JkDependencySet, JkDependencySet> dependenciesModifier = deps -> deps;

    private Supplier<JkDependencySet> dependencyBootSupplier = () -> JkDependencySet.of();

    private final LinkedList<String> extraJavaCompilerOptions = new LinkedList<>();

    private List<JkSourceGenerator> sourceGenerators = new LinkedList<>();

    private boolean done;

    // label to mention the purpose of the compilation (production, test, ...)
    private String purpose;

    private Supplier<JkJavaCompileSpec> compileSpecSupplier;

    private JkProjectCompilation(JkProjectConstruction construction, String purpose, T parent) {
        __ = parent;
        this.purpose = purpose;
        this.construction = construction;
        resourceProcessor = JkResourceProcessor.ofParent(this);
        preCompileActions = JkRunnables.ofParent(this)
                .setLogRunnableName(true)
                .append(RESOURCES_PROCESS_ACTION, this::processResources);
        compileActions = JkRunnables.ofParent(this)
                .setLogRunnableName(true)
                .append(JAVA_SOURCES_COMPILE_ACTION, this::compileJava);
        postCompileActions = JkRunnables.ofParent(this)
                .setLogRunnableName(true);

        layout = JkCompileLayout.ofParent(this)
                .setBaseDirSupplier(construction.getProject()::getBaseDir)
                .setOutputDirSupplier(construction.getProject()::getOutputDir);
    }

    static JkProjectCompilation<JkProjectConstruction> ofProd(JkProjectConstruction projectProduction) {
        JkProjectCompilation result =
                new JkProjectCompilation(projectProduction, PRODUCTION_PURPOSE, projectProduction);
        result.compileSpecSupplier = () -> result.computeProdCompileSpec();
        return result;
    }

    static JkProjectCompilation<JkProjectTesting> ofTest(JkProjectConstruction construction,
                                                         JkProjectTesting parent) {
        JkProjectCompilation result =
                new JkProjectCompilation(construction, TEST_PURPOSE, parent);
        result.dependencyBootSupplier = () -> construction.getRuntimeDependencies().merge(construction
            .getCompilation().getDependencies()).getResult();
        result.compileSpecSupplier = () -> result.computeTestCompileSpec(construction.getCompilation());
        result.layout
                .setSourceMavenStyle(JkCompileLayout.Concern.TEST)
                .setStandardOutputDirs(JkCompileLayout.Concern.TEST);
        return result;
    }

    public JkProjectCompilation apply(Consumer<JkProjectCompilation> consumer) {
        consumer.accept(this);
        return this;
    }

    public JkCompileLayout<JkProjectCompilation<T>> getLayout() {
        return layout;
    }

    public void generateSources() {
        for (JkSourceGenerator sourceGenerator : sourceGenerators) {
            JkLog.startTask("Generate sources with " + sourceGenerator);
            Path path = layout.resolveGeneratedSourceDir().resolve(sourceGenerator.getDirName());
            sourceGenerator.generate(path);
            JkLog.endTask();
        }
    }

    /**
     * Performs entire compilation phase.
     */
    public void run() {
        JkLog.startTask("Run whole compilation process for " + purpose);
        generateSources();
        preCompileActions.run();
        compileActions.run();
        postCompileActions.run();
        JkLog.endTask();
    }

    /**
     * As #run but perform only if not already done.
     */
    public void runIfNeeded() {
        if (done) {
            JkLog.trace(JAVA_SOURCES_COMPILE_ACTION + " already done. Won't perform again.");
        } else {
            run();
            done = true;
        }
    }

    /**
     * Returns the {@link JkRunnables} to run after source and resource generation. User can chain its own runnable
     * to customise the process. Contains {@link JkProjectCompilation#RESOURCES_PROCESS_ACTION} by default
     */
    public JkRunnables<JkProjectCompilation<T>> getPreCompileActions() {
        return preCompileActions;
    }

    /**
     * Returns the {@link JkRunnables} to be run after compilation. User can chain its own runnable
     * to customise the process. Empty by default.
     */
    public JkRunnables<JkProjectCompilation<T>> getPostCompileActions() {
        return postCompileActions;
    }

    /**
     * Returns extra compile options passed to the compiler
     */
    public List<String> getExtraJavaCompilerOptions() {
        return Collections.unmodifiableList(extraJavaCompilerOptions);
    }

    /**
     * Adds options to be passed to Java compiler
     */
    public JkProjectCompilation<T> addJavaCompilerOptions(String ... options) {
        this.extraJavaCompilerOptions.addAll(Arrays.asList(options));
        return this;
    }

    /**
     * Returns the resource processor.
     */
    public JkResourceProcessor<JkProjectCompilation<T>> getResourceProcessor() {
        return resourceProcessor;
    }

    public JkProjectCompilation<T> configureDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        this.dependenciesModifier = dependenciesModifier.andThen(modifier);
        return this;
    }

    public JkResolveResult resolveDependencies() {
        return construction.getDependencyResolver().resolve(getDependencies());
    }

    public JkDependencySet getDependencies() {
        return dependenciesModifier.apply(dependencyBootSupplier.get());
    }

    public JkProjectCompilation<T> addSourceGenerator(JkSourceGenerator sourceGenerator) {
        this.sourceGenerators.add(sourceGenerator);
        return this;
    }

    public List<Path> getGeneratedSourceDirs() {
        return sourceGenerators.stream()
                .map(sourceGenerator ->
                        layout.resolveGeneratedSourceDir().resolve(sourceGenerator.getDirName()))
                .collect(Collectors.toList());
    }

    private void processResources() {
        this.getResourceProcessor().generate(layout.resolveResources(), layout.resolveClassDir());
    }

    private void compileJava() {
        boolean success = construction.getCompiler().compile(compileSpecSupplier.get());
        if (!success) {
            throw new IllegalStateException("Compilation of Java sources failed.");
        }
    }

    private JkJavaCompileSpec computeProdCompileSpec() {
        return JkJavaCompileSpec.of()
            .setSourceAndTargetVersion(construction.getJvmTargetVersion())
            .setEncoding(construction.getSourceEncoding())
            .setClasspath(resolveDependencies().getFiles())
            .setSources(layout.resolveSources().and(getGeneratedSourceDirs().toArray(new Path[0])))
            .addOptions(extraJavaCompilerOptions)
            .setOutputDir(layout.resolveClassDir());
    }

    private JkJavaCompileSpec computeTestCompileSpec(JkProjectCompilation prodStep) {
        JkDependencySet dependencies = getDependencies();
        return JkJavaCompileSpec.of()
                .setSourceAndTargetVersion(construction.getJvmTargetVersion())
                .setEncoding(construction.getSourceEncoding())
                .setClasspath(construction.getDependencyResolver().resolve(dependencies).getFiles()
                            .andPrepend(prodStep.layout.resolveClassDir()))
                .setSources(layout.resolveSources().and(getGeneratedSourceDirs().toArray(new Path[0])))
                .addOptions(extraJavaCompilerOptions)
                .setOutputDir(layout.resolveClassDir());
    }

}
