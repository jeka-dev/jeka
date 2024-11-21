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

package dev.jeka.core.tool.builtins.base;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.tooling.git.JkVersionFromGit;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkException;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.api.project.JkBuildable;
import dev.jeka.core.tool.builtins.scaffold.JkScaffoldOptions;
import dev.jeka.core.tool.builtins.tooling.git.JkGitVersioning;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@JkDoc(
        "Manages the build and execution of code located in 'jeka-src' directory\n" +
        "The application must contain a class with main method in order to : \n" +
        "  - Run application\n" +
        "  - Create bootable jar\n" +
        "  - Create bootable fat jar, and create Docker images."
)
public final class BaseKBean extends KBean {

    public static final String CREATE_JAR_ACTION = "create-jar";

    @JkDoc("Space separated list of options to pass to the JVM that will run the program.")
    public String jvmOptions = "";

    @JkDoc("Space separated list of program arguments to pass to the command line running the program.")
    public String programArgs = "";

    @JkDoc("module group and name used for publication. Formatted as 'groupId:nameId'")
    public String moduleId;

    public JkGitVersioning gitVersioning = JkGitVersioning.of();

    @JkDoc
    final BaseScaffoldOptions scaffold = new BaseScaffoldOptions();

    /**
     * Actions to execute when {@link BaseKBean#pack()} is invoked.<p>
     * By default, the build action creates a fat jar. It can be
     * replaced by an action creating other jars/artifacts or doing special
     * action as publishing a Docker image, for example.
     */
    public final JkRunnables packActions = JkRunnables.of();

    public final JkConsumers<JkManifest> manifestCustomizers = JkConsumers.of();

    private Supplier<String> mainClassFinder = this::findMainClass;

    private JkModuleId module;

    private Supplier<JkVersion> versionSupplier = () -> JkVersion.UNSPECIFIED;

    private Consumer<Path> jarMaker = this::fatJar;

    private JkBaseScaffold baseScaffold;

    @Override
    protected void init() {
        baseScaffold = JkBaseScaffold.of(this);
        packActions.append(CREATE_JAR_ACTION, this::buildJar);
        if (!JkUtilsString.isBlank(moduleId)) {
            setModuleId(this.moduleId);
        }
        if (gitVersioning.enable) {
            JkVersionFromGit.of(getBaseDir(), gitVersioning.tagPrefix).handleVersioning(this);
        }
    }

    // We can not just run Application#main cause Spring-Boot seems
    // requiring that the Java process is launched using Spring-Boot application class
    @JkDoc("Launches application")
    public void runMain() {
        Path tempDirClass = JkUtilsPath.createTempDirectory("jk-");
        getAppClasses().copyTo(tempDirClass);
        JkJavaProcess.ofJava(getMainClass())
                .setClasspath(JkPathSequence.of(tempDirClass).and(getAppClasspath()))
                .setInheritIO(true)
                //.setInheritSystemProperties(true)
                .setDestroyAtJvmShutdown(true)
                .addJavaOptions(JkUtilsString.parseCommandline(jvmOptions))
                .addParams(JkUtilsString.parseCommandline(programArgs))
                .exec();
        JkPathTree.of(tempDirClass).createIfNotExist();
    }

    @JkDoc("Launches test suite")
    public void test() {
        if (!JkTestProcessor.isEngineTestPresent()) {
            throw new JkException("No engine test class found in current classloader. " +
                    "You should add @JkDep(\"org.junit.jupiter:junit-jupiter\") dependencies" +
                    "to the classpath for testing.");
        }
        JkTestSelection testSelection = JkTestSelection.of().addTestClassRoots(getAppClasses().getRoot());
        JkTestProcessor.of()
                .setForkingProcess(true)
                .launch(JkClassLoader.ofCurrent().getClasspath(), testSelection);
    }

    @JkDoc("Creates runnable fat jar and optional artifacts.")
    public void pack() {
        packActions.run();
    }

    @JkDoc("Runs fat jar.")
    public void runJar() {
        this.prepareRunJar().exec();
    }

    @JkDoc("Displays info about this SelfApp")
    public void info() {
        StringBuilder sb = new StringBuilder();
        sb.append("Module Id    : " + this.module).append("\n");
        sb.append("Version      : " + this.getVersion()).append("\n");
        sb.append("Main Class   : " + this.getMainClass()).append("\n");
        sb.append("JVM Options  : " + jvmOptions).append("\n");
        sb.append("Program Args : " + programArgs).append("\n");
        sb.append("Class Files  : ").append("\n");
        this.getAppClasses().getRelativeFiles().stream()
                .forEach(path -> sb.append("  " + path + "\n"));
        sb.append("Classpath    : ").append("\n");
        this.getAppClasspath().forEach(path -> sb.append("  " + path + "\n"));
        sb.append("Manifest     : ").append("\n");
        Arrays.stream(getManifest().asString().split("\n"))
                .forEach(line -> sb.append("  " + line + "\n"));
        JkLog.info(sb.toString());
    }

    @JkDoc("Displays exported dependency tree on console.")
    public void depTree() {
        JkDependencySet deps = getRunbase().getExportedDependencies()
                .andVersionProvider(JkConstants.JEKA_VERSION_PROVIDER);
        String output = getRunbase().getDependencyResolver().resolve(deps)
                .getDependencyTree().toStringTree();
        JkLog.info(output);
    }

    @JkDoc("Creates a skeleton in the current working directory.")
    public void scaffold() {
        baseScaffold.run();
    }

    /**
     * Builds a JAR file using the provided jarMaker
     */
    public BaseKBean buildJar() {
        jarMaker.accept(getJarPath());
        return this;
    }

    /**
     * Returns the path to the JAR file created by {@link #buildJar()} method.
     */
    public Path getJarPath() {
        return Paths.get(getJarPathBaseName() + ".jar");
    }

    /**
     * Returns the base name of the JAR file path created by the {@link #buildJar()} method.
     */
    public String getJarPathBaseName() {
        return getBaseDir().resolve(JkConstants.OUTPUT_PATH).resolve(getBaseDirName()).toString();
    }

    /**
     * Sets the jarMaker for creating a runnable fat jar.
     */
    public BaseKBean setJarMaker(Consumer<Path> jarMaker) {
        this.jarMaker = jarMaker;
        return this;
    }

    /**
     * Sets the version supplier for this BaseKBean. The version represents the
     * version of the application/library.
     */
    public BaseKBean setVersionSupplier(Supplier<JkVersion> versionSupplier) {
        this.versionSupplier = versionSupplier;
        return this;
    }

    /**
     * Sets the version for this BaseKBean. The version represents the
     * version of the application/library.
     */
    public BaseKBean setVersion(String version) {
        this.versionSupplier = ()  -> JkVersion.of(version);
        return this;
    }

    /**
     * Returns the version of the application/library. The version might be used in Manifest file,
     * docker image name or Maven publication.
     */
    public JkVersion getVersion() {
        return versionSupplier.get();
    }

    /**
     * Returns the module ID for this object.
     * The module ID is used for naming Docker image and in Maven publication.
     */
    public JkModuleId getModuleId() {
        return module;
    }

    /**
     * Sets the module ID for this object. The module id might be used for naming Docker image
     * and in Maven publication.
     *
     * @param moduleId The module ID formatted as <i>group:name</i>.
     */
    public BaseKBean setModuleId(String moduleId) {
        this.module = JkModuleId.of(moduleId);
        return this;
    }

    /**
     * Returns the actual main class to be used for launching the application or executable JAR.
     * This method returns <code>null</code> if no main class has been detected.
     */
    public String getMainClass() {
        return mainClassFinder.get();
    }

    /**
     * Returns the JkBaseScaffold object associated with this BaseKBean.
     * The JkBaseScaffold provides methods for configuring the project scaffold,
     * such as adding file entries and setting options.
     */
    public JkBaseScaffold getBaseScaffold() {
        return baseScaffold;
    }

    /**
     * Sets the main class finder for this project. The main class finder is responsible for
     * providing the name of the main class to use in the project. This can be used for running
     * the project or building Docker images.
     */
    public BaseKBean setMainClassFinder(Supplier<String> mainClassFinder) {
        this.mainClassFinder = mainClassFinder;
        return this;
    }

    /**
     * Returns the application classpath. This contains class dir + libraries.
     */
    public List<Path> getAppClasspath() {
        return getRunbase().getExportedClasspath().getEntries();
    }

    /**
     * Returns a List of Path objects representing the libraries used by the application.
     * It contains the classpath minus the class dir.
     */
    public List<Path> getAppLibs() {
        return getAppClasspath().stream()
                .filter(entry -> !entry.toAbsolutePath().normalize()
                        .equals(getAppClasses().getRoot().toAbsolutePath().normalize()))
                .collect(Collectors.toList());
    }

    /**
     * Returns a {@link JkPathTree} representing the class files and sub-folders contained in the "bin" directory.
     * The tree includes all files in the root directory and its subdirectories,
     * except for files matching the specified patterns.
     */
    public JkPathTree getAppClasses() {
        return JkPathTree.of(getBaseDir().resolve(JkConstants.JEKA_SRC_CLASSES_DIR))
                .andMatching(false, "_*", "_*/**", ".*", "**/.*");
    }





    /**
     * Returns the {@link JkManifest} for the application.
     * The manifest includes the created by attribute,
     * the main class attribute, and the build JDK attribute.
     */
    public JkManifest getManifest() {
        JkManifest manifest = JkManifest.of()
                .addImplementationInfo(getModuleId(), getVersion())
                .addMainClass(getMainClass())
                .addBuildInfo();
        manifestCustomizers.accept(manifest);
        return manifest;
    }

    /**
     * Creates a main JAR file at the specified target path.
     */
    public void createMainJar(Path target) {
        jarMaker.accept(target);
    }

    /**
     * Creates a Javadoc Jar file at the specified target path.
     */
    public void createJavadocJar(Path target) {
        JkUtilsPath.deleteIfExists(target);
        Path tempFolder = JkUtilsPath.createTempDirectory("jk-self-sources");
        JkJavadocProcessor.of()
                .make(getAppLibs(), getAppSources().toSet(), tempFolder);
        JkPathTree.of(tempFolder).zipTo(target);
        JkPathTree.of(tempFolder).deleteRoot();
    }

    /**
     * Creates a source JAR file at the specified target path.
     */
    public void createSourceJar(Path target) {
        JkUtilsPath.deleteIfExists(target);
        getAppSources().zipTo(target);
    }

    /**
     * Returns a JkPathTree representing the application sources.
     */
    public JkPathTree getAppSources() {
        return JkPathTree.of(getBaseDir().resolve(JkConstants.JEKA_SRC_DIR)).withMatcher(
                JkConstants.PRIVATE_IN_DEF_MATCHER.negate());
    }

    /**
     * Prepares a {@link JkJavaProcess ready to run.
     */
    public JkJavaProcess prepareRunJar() {
        Path jarPath = getJarPath();
        if (!Files.exists(jarPath)) {
            buildJar();
        }
        return JkJavaProcess.ofJavaJar(getJarPath())
                .setLogCommand(true)
                .setInheritIO(true)
                .setDestroyAtJvmShutdown(true)
                .addJavaOptions(JkUtilsString.parseCommandline(jvmOptions))
                .addParams(JkUtilsString.parseCommandline(programArgs));
    }

    private String findMainClass() {
        JkUrlClassLoader ucl = JkUrlClassLoader.of(getBaseDir().resolve(JkConstants.JEKA_SRC_CLASSES_DIR));
        return ucl.toJkClassLoader().findClassesHavingMainMethod().stream()
                .filter(candidate -> !candidate.startsWith("_"))
                .findFirst()
                .orElse("dev.jeka.core.tool.Main");
    }

    private void fatJar(Path jarPath) {
        JkLog.startTask("Making fat jar. It may takes a while");
        JkJarPacker.of(getAppClasses())
                .withManifest(getManifest())
                .makeFatJar(jarPath, getAppLibs(), JkPathMatcher.of());
        JkLog.endTask();
        JkLog.info("Jar created at : " + jarPath);
    }

    public static class BaseScaffoldOptions extends JkScaffoldOptions {

        @JkDoc("Kind of Jeka base to generate.")
        public JkBaseScaffold.Kind kind = JkBaseScaffold.Kind.JEKA_SCRIPT;

    }

    public JkBuildable asBuildable() {

        return new JkBuildable() {

            @Override
            public Path getClassDir() {
                Path tempDirClass = JkUtilsPath.createTempDirectory("jk-");
                BaseKBean.this.getAppClasses().copyTo(tempDirClass);
                return tempDirClass;
            }

            @Override
            public JkResolveResult resolveRuntimeDependencies() {
                JkDependencySet deps = getRunbase().getExportedDependencies()
                        .andVersionProvider(JkConstants.JEKA_VERSION_PROVIDER);
                return getRunbase().getDependencyResolver().resolve(deps);
            }

            @Override
            public List<Path> getRuntimeDependenciesAsFiles() {
                return BaseKBean.this.getAppClasspath();
            }

            @Override
            public JkVersion getVersion() {
                return BaseKBean.this.getVersion();
            }

            @Override
            public JkModuleId getModuleId() {
                return BaseKBean.this.getModuleId();
            }

            @Override
            public Path getOutputDir() {
                return BaseKBean.this.getOutputDir();
            }

            @Override
            public Path getBaseDir() {
                return BaseKBean.this.getBaseDir();
            }

            @Override
            public String getMainClass() {
                return BaseKBean.this.getMainClass();
            }

            @Override
            public void compileIfNeeded() {
                // base source are always compiled when running
            }

            @Override
            public JkDependencyResolver getDependencyResolver() {
                return getRunbase().getDependencyResolver();
            }

            @Override
            public Path getMainJarPath() {
                return BaseKBean.this.getJarPath();
            }

            @Override
            public Adapted getAdapted() {
                return Adapted.BASE;
            }

            @Override
            public boolean compile(JkJavaCompileSpec compileSpec) {
                return JkJavaCompilerToolChain.of().compile(compileSpec);
            }

            public String toString() {
                return BaseKBean.this.toString();
            }
        };
    }



}