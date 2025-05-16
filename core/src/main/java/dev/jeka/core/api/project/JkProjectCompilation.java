/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkDependencySetModifier;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkResourceProcessor;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompilerToolChain;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Handles project compilation step. Users can configure inner phases by chaining runnables.
 * They also can modify {@link JkJavaCompilerToolChain} and {@link JkJavaCompileSpec} to use.
 */
public class JkProjectCompilation {

    public static final String RESOURCES_PROCESS_ACTION = "process-resources";

    public static final String JAVA_SOURCES_COMPILE_ACTION = "compile-java-sources";

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

    public final JkDependencySetModifier dependencies = JkDependencySetModifier.of()
            .modify(deps -> baseDependencies());

    private final LinkedList<String> extraJavaCompilerOptions = new LinkedList<>();

    private final List<JkProjectSourceGenerator> sourceGenerators = new LinkedList<>();

    private boolean done;

    JkProjectCompilation(JkProject project) {
        this.project = project;
        resourceProcessor = JkResourceProcessor.of();
        preCompileActions = JkRunnables.of()
                .setLogTasks(true)
                .setTaskSuffix(taskSuffix())
                .append(RESOURCES_PROCESS_ACTION, this::processResources);
        compileActions = JkRunnables.of()
                .setLogTasks(true)
                .setTaskSuffix(taskSuffix())
                .append(JAVA_SOURCES_COMPILE_ACTION, this::compileJava);
        postCompileActions = JkRunnables.of()
                .setTaskSuffix(taskSuffix())
                .setLogTasks(true);
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
        if (!sourceGenerators.isEmpty()) {
            JkLog.startTask("generate-sources");
        }
        for (JkProjectSourceGenerator sourceGenerator : sourceGenerators) {
            Path path = layout.resolveGeneratedSourceDir().resolve(sourceGenerator.getDirName());
            sourceGenerator.generate(this.project, path);
            JkLog.info("Sources generated in " + path);
        }
        if (!sourceGenerators.isEmpty()) {
            JkLog.endTask();
        }
    }

    /**
     * Performs entire compilation phase.
     */
    public void run() {
        generateSources();
        preCompileActions.run();
        compileActions.run();
        postCompileActions.run();
        done = true;
    }

    /**
     * As #run but perform only if not already done.
     */
    public void runIfNeeded() {
        if (done) {
            JkLog.debug(JAVA_SOURCES_COMPILE_ACTION + " already done. Won't perform again.");
        } else {
            run();
        }
    }

    public void skipJavaCompilation() {
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

    public List<Path> resolveDependenciesAsFiles() {
        return project.dependencyResolver.resolveFiles(dependencies.get());
    }

    /**
     * Adds a source generator responsible for generating source files to the project compilation process.
     *
     * @param sourceGenerator the {@link JkProjectSourceGenerator} instance used for generating sources
     * @return the current instance of {@link JkProjectCompilation} to allow method chaining
     */
    public JkProjectCompilation addSourceGenerator(JkProjectSourceGenerator sourceGenerator) {
        this.sourceGenerators.add(sourceGenerator);
        return this;
    }

    /**
     * Adds a source generator responsible for generating source files to the project compilation process.
     * The generator is defined by a name and a {@link BiConsumer} action that takes a {@link JkProject}
     * and a {@link Path} representing the generated source directory.
     *
     * @param generatorName the name assigned to the source generator
     * @param generator a {@link BiConsumer} defining the generation behavior by taking a {@link JkProject}
     *                  instance and the directory where sources will be generated
     * @return the current instance of {@link JkProjectCompilation} to allow method chaining
     */
    public JkProjectCompilation addSourceGenerator(String generatorName, BiConsumer<JkProject, Path> generator) {
        return addSourceGenerator(new JkProjectSourceGenerator() {

            @Override
            public String getDirName() {
                return generatorName;
            }

            @Override
            public void generate(JkProject project, Path generatedSourceDir) {
                generator.accept(project, generatedSourceDir);
            }

        });
    }

    /**
     * Adds a source generator responsible for generating source files to the project compilation process.
     * The generator is defined by a name and an action that takes a directory where sources will be generated.
     *
     * @param generatorName the name assigned to the source generator
     * @param generator a {@link Consumer} defining the generation behavior by accepting the directory
     *                  where sources will be generated
     * @return the current instance of {@link JkProjectCompilation} to allow method chaining
     */
    public JkProjectCompilation addSourceGenerator(String generatorName, Consumer<Path> generator) {
        return addSourceGenerator(generatorName, (project, generatedSourceDir)
                -> generator.accept(generatedSourceDir));
    }

    public boolean isDone() {
        return done;
    }

    public List<Path> getGeneratedSourceDirs() {
        return sourceGenerators.stream()
                .map(sourceGenerator ->
                        layout.resolveGeneratedSourceDir().resolve(sourceGenerator.getDirName()))
                .collect(Collectors.toList());
    }

    boolean isCompilationForked() {
        return project.compilerToolChain.isCompilationForked(project.getJvmTargetVersion(), compileSpec());
    }

    private void processResources() {
        this.resourceProcessor.generate(layout.resolveResources(), layout.resolveClassDir());
    }

    private void compileJava() {
        JkUtilsPath.createDirectories(getGeneratedSourcesForAnnoProcessors());
        JkJavaVersion javaVersion = project.getJvmTargetVersion() == null ? JkJavaVersion.ofCurrent()
                : project.getJvmTargetVersion();
        JkJavaCompileSpec compileSpec = compileSpec();
        if (javaVersion.isEqualOrGreaterThan(21) && !compileSpec.getOptions().contains("-proc:full")) {
            compileSpec.addOptions("-proc:full");
        }
        JkJavaCompilerToolChain.Status status = project.compilerToolChain.compile(project.getJvmTargetVersion(), compileSpec);
        if (!JkLog.isVerbose() && status == JkJavaCompilerToolChain.Status.SUCCESS) {
            JkLog.info("Succeed");  // must log something otherwise next task will indent badly
        }
        if (status == JkJavaCompilerToolChain.Status.FAILED) {
            throw new IllegalStateException("Compilation of Java sources failed.");
        }
    }

    private JkJavaCompileSpec compileSpec() {
        return JkJavaCompileSpec.of()
            .setEncoding(project.getSourceEncoding())
            .setClasspath(classpath())
            .setSources(layout.resolveSources().and(getGeneratedSourceDirs().toArray(new Path[0])))
            .addOptions(extraJavaCompilerOptions)
            .addOptions("-s", getGeneratedSourcesForAnnoProcessors().toString())
            .setOutputDir(layout.resolveClassDir());
    }

    private Path getGeneratedSourcesForAnnoProcessors() {
        return project.getBaseDir().resolve(JkConstants.OUTPUT_PATH + "/" + JkCompileLayout.GENERATED_SOURCE_PATH
                + "/annotation-processors");
    }

    // -------- methods to override for test compilation

    protected JkCompileLayout initialLayout() {
        return JkCompileLayout.of()
                .setBaseDirSupplier(project::getBaseDir)
                .setOutputDirSupplier(project::getOutputDir);
    }

    protected JkPathSequence classpath() {
        return JkPathSequence.of(resolveDependenciesAsFiles());
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

    private String taskSuffix() {
        return "production".equals(this.purpose()) ? "" : " (" + this.purpose() + ")"
                + project.relativeLocationLabel();
    }

}
