package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathSequence;
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
import java.util.stream.Collectors;

/**
 * Handles project compilation step. Users can configure inner phases by chaining runnables.
 * They also can modify {@link JkJavaCompiler} and {@link JkJavaCompileSpec} to use.
 */
public class JkProjectCompilation {

    public static final String RESOURCES_PROCESS_ACTION = "resources-process";

    public static final String JAVA_SOURCES_COMPILE_ACTION = "java-sources-compile";

    private final JkProject project;

    /**
     * The {@link JkRunnables} to run after source and resource generation. User can chain its own runnable
     * to customise the process. Contains {@link JkProjectCompilation#RESOURCES_PROCESS_ACTION} by default
     */
    public final JkRunnables preCompileActions;


    private final JkRunnables compileActions;

    /**
     * The {@link JkRunnables} to be run after compilation. User can chain its own runnable
     * to customise the process. Empty by default.
     */
    public final JkRunnables postCompileActions;

    public final JkResourceProcessor resourceProcessor;

    public final JkCompileLayout layout;

    private Function<JkDependencySet, JkDependencySet> dependenciesModifier = deps -> deps;

    private final LinkedList<String> extraJavaCompilerOptions = new LinkedList<>();

    private List<JkSourceGenerator> sourceGenerators = new LinkedList<>();

    private boolean done;

    private JkResolveResult cachedResolveResult = null;

    JkProjectCompilation(JkProject project) {
        this.project = project;
        resourceProcessor = JkResourceProcessor.of();
        preCompileActions = JkRunnables.of()
                .setLogRunnableName(true)
                .append(RESOURCES_PROCESS_ACTION, this::processResources);
        compileActions = JkRunnables.of()
                .setLogRunnableName(true)
                .append(JAVA_SOURCES_COMPILE_ACTION, this::compileJava);
        postCompileActions = JkRunnables.of()
                .setLogRunnableName(true);
        layout = initialLayout();
    }

    static JkProjectCompilation ofProd(JkProject project) {
        return new JkProjectCompilation(project);
    }

    public JkProjectCompilation apply(Consumer<JkProjectCompilation> consumer) {
        consumer.accept(this);
        return this;
    }


    public void generateSources() {
        for (JkSourceGenerator sourceGenerator : sourceGenerators) {
            JkLog.startTask("Generate sources with " + sourceGenerator);
            Path path = layout.resolveGeneratedSourceDir().resolve(sourceGenerator.getDirName());
            sourceGenerator.generate(this.project, path);
            JkLog.endTask();
        }
    }

    /**
     * Performs entire compilation phase.
     */
    public void run() {
        JkLog.startTask("Run compilation phase for " + purpose());
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

    public void skipJavaCompiilation() {
        this.compileActions.remove(JAVA_SOURCES_COMPILE_ACTION);
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
    public JkProjectCompilation addJavaCompilerOptions(String ... options) {
        this.extraJavaCompilerOptions.addAll(Arrays.asList(options));
        return this;
    }

    public JkProjectCompilation configureDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        this.dependenciesModifier = dependenciesModifier.andThen(modifier);
        return this;
    }

    public JkResolveResult resolveDependencies() {
        if (cachedResolveResult != null) {
            return cachedResolveResult;
        }
        cachedResolveResult = project.dependencyResolver.resolve(getDependencies());
        return cachedResolveResult;
    }

    public JkDependencySet getDependencies() {
        return dependenciesModifier.apply(baseDependencies());
    }

    public JkProjectCompilation addSourceGenerator(JkSourceGenerator sourceGenerator) {
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
        this.resourceProcessor.generate(layout.resolveResources(), layout.resolveClassDir());
    }

    private void compileJava() {
        boolean success = project.compiler.compile(compileSpec());
        if (!success) {
            throw new IllegalStateException("Compilation of Java sources failed.");
        }
    }

    private JkJavaCompileSpec compileSpec() {
        return JkJavaCompileSpec.of()
            .setEncoding(project.getSourceEncoding())
            .setClasspath(classpath())
            .setSources(layout.resolveSources().and(getGeneratedSourceDirs().toArray(new Path[0])))
            .addOptions(extraJavaCompilerOptions)
            .setOutputDir(layout.resolveClassDir());
    }

    // -------- methods to override for test compilation

    protected JkCompileLayout initialLayout() {
        return JkCompileLayout.of()
                .setBaseDirSupplier(project::getBaseDir)
                .setOutputDirSupplier(project::getOutputDir);
    }

    protected JkPathSequence classpath() {
        return resolveDependencies().getFiles();
    }

    protected JkDependencySet baseDependencies() {
        if (project.isIncludeTextAndLocalDependencies()) {
            return project.textAndLocalDeps().getCompile();
        }
        return JkDependencySet.of();
    }

    protected String purpose() {
        return "production";
    }

}
