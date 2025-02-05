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

package dev.jeka.core.tool.builtins.project;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.java.JkJavaCompilerToolChain;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.project.*;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.tooling.git.JkVersionFromGit;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.scaffold.JkScaffoldOptions;
import dev.jeka.core.tool.builtins.tooling.git.JkGitVersioning;
import org.w3c.dom.Document;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@JkDoc("Manages the build and execution of a JVM projects.\n" +
        "It contains all information for resolving dependencies, compiling, testing and packaging as JARs")
public final class ProjectKBean extends KBean implements JkIdeSupportSupplier, JkBuildable.Supplier {

    // The underlying project managed by this KBean
    @JkDoc(hide = true)
    public final JkProject project = JkProject.of();

    @JkDoc("Version of the project. Can be used by a CI/CD tool to inject version.")
    public String version;

    @JkDoc("Module id of the project. Only needed if the project is published on a Maven repository.")
    public String moduleId;

    @JkDoc("The encoding format used for handling source files within the project.")
    public String sourceEncoding;

    /**
     * Options for the packaging tasks (jar creation). These options are injectable from command line.
     */
    @JkDoc
    public final JkPackOptions pack = new JkPackOptions();

    /**
     * Options for run tasks
     */
    public final JkRunOptions run = new JkRunOptions();

    @JkDoc
    private final JkDependenciesOptions dependencies = new JkDependenciesOptions();

    /**
     * Options for configuring testing tasks.
     */
    // Made public so that extension as springboot can override default
    @JkDoc
    public final JkTestOptions tests = new JkTestOptions();

    /**
     * Options for configuring scaffold.
     */
    public final JkProjectScaffoldOptions scaffold = new JkProjectScaffoldOptions();

    /**
     * Options for configuring directory layout.
     */
    @JkDoc
    public final JkLayoutOptions layout = new JkLayoutOptions();

    /**
     * Options for configuring compilation.
     */
    @JkDoc
    @JkPropValue // some nested fields are prop-injectable
    public final JkCompilationOptions compilation = new JkCompilationOptions();

    @JkDoc("The output file for the xml dependency description.")
    public Path outputFile;

    public final JkGitVersioning gitVersioning = JkGitVersioning.of();

    private JkProjectScaffold projectScaffold;

    @JkDoc("Applies the specified configuration to the underlying `JkProject` instance.")
    @Override
    protected void init() {
        configureProject();
    }

    // ------------------------------- command line methods -----------------------------

    @JkDoc("Deletes the content of jeka-output directory and might execute extra clean actions")
    public void clean() {
        project.clean();
    }

    @JkDoc("Generates sources")
    public void generateSources() {
        project.compilation.generateSources();
    }

    @JkDoc("Performs compilation and resource processing")
    public void compile() {
        project.compilation.run();
    }

    @JkDoc("Compiles and run tests defined within the project (typically Junit tests)")
    public void test() {    //NOSONAR
        project.testing.run();
    }

    @JkDoc("Generates from scratch artifacts defined through 'pack' options if not yet generated. " +
            "Use #cleanPack to force re-generation.")
    public void pack() {   //NOSONAR
        project.pack();
    }

    @JkDoc("Displays resolved dependency trees on console.")
    public void depTree() {
        project.displayDependencyTree();
    }

    @JkDoc("Displays resolved dependency trees as xml, on console.")
    public void depTreeAsXml() {
        Document document = project.getDependenciesAsXml();
        String xml = JkDomDocument.of(document).toXml();
        if (outputFile == null) {
           JkLog.info(xml);
        } else {
            JkPathFile.of(outputFile).createIfNotExist().write(xml);
        }
    }

    @JkDoc("Displays information about the Java project to build.")
    public void info() {
        JkLog.info(this.project.getInfo());
        JkLog.info("Execute 'project: depTree' to display dependency resolution.");
    }

    @JkDoc("Runs the generated jar.")
    public void runJar() {
        this.run.runJar();
    }

    @JkDoc("Runs the compiled classes.")
    public void runMain() {
        this.run.runMain();
    }
    
    @JkDoc("Scaffolds a JeKa project skeleton in working directory.")
    public void scaffold() {
        projectScaffold.run();
    }

    @Override
    public JkIdeSupport getJavaIdeSupport() {
        return project.getJavaIdeSupport();
    }

    /**
     * Returns the project scaffold used by this KBean to perform #scaffold#
     */
    public JkProjectScaffold getProjectScaffold() {
        return projectScaffold;
    }

    @Override
    public JkBuildable asBuildable() {
        return project.asBuildable();
    }

    // ------- static classes for configuration

    /**
     * Standard options for packaging java projects.
     */
    public static class JkPackOptions {

        @JkDoc("Type of jar to produce for the main artifact.")
        public JkProjectPackaging.JarType jarType = JkProjectPackaging.JarType.REGULAR;

        @JkDoc("If not blank, the project will produce an extra shade jar having the specified classifier name.\n" +
                "A shade Jar embeds classes coming from dependency jars. The dependency class packages are relocated to " +
                "avoid potential collisions with other jar present in the classpath.")
        @JkDepSuggest(versionOnly = true, hint = "uber,all")
        public String shadeJarClassifier;

        @JkDoc("Main class name to include in Manifest. Use 'auto' to automatic discovering.")
        public String mainClass;

        @JkDoc("If true and no mainClass specified, it will be detected and added to the Manifest.")
        public boolean detectMainClass;

    }

    /**
     * Options about tests
     */
    public static final class JkTestOptions {

        @JkDoc("Space-separated string to filter the test class names to run. " +
                "Use regex patterns like '.*', '.*Test', '.*IT', or 'ac.me.MyTest'.")
        public String includePatterns = null;//".*";

        /** Turn it on to skip tests. */
        @JkDoc("If true, tests are not run.")
        @JkPropValue("jeka.test.skip")
        public boolean skip;

        /** Turn it on to run tests in a withForking process. */
        @JkDoc("If true, tests will be executed in a forked process.")
        public boolean fork = true;

        /** Argument passed to the JVM if tests are withForking. Example : -Xms2G -Xmx2G */
        @JkDoc("Argument passed to the JVM if tests are executed in a forked process (example -Xms2G -Xmx2G).")
        public String jvmOptions;

        @JkDoc("The style to use to show test execution progress.")
        // Should be default to null, has other kbean can check if
        // the value has explicitly been set.
        public JkTestProcessor.JkProgressStyle progress;

        @JkDoc("The junit-platform to use. If empty, the default version will be used (1.9.3).")
        @JkDepSuggest(versionOnly = true, hint="org.junit.platform:junit-platform-commons")
        public String platformVersion;

    }

    public static class JkLayoutOptions {

        @JkDoc("Style of directory source structure (src/main/java or just src)")
        public JkCompileLayout.Style style = JkCompileLayout.Style.MAVEN;

        @JkDoc("If true, Resource files are located in same folder than Java code.")
        public boolean mixSourcesAndResources = false;

    }

    public static class JkCompilationOptions {

        @JkDoc("Specify whether to fork the compilation process.")
        public boolean fork;

        @JkDoc("The target JVM version for compiled files.")
        @JkPropValue("jeka.java.version")
        public String javaVersion;

        @JkDoc("Extra arguments to be passed to the compiler (example -Xlint:unchecked).")
        @JkDepSuggest(versionOnly = true, hint = "-Xlint,-Xlint:deprecation,-Xlint:unchecked")
        public String compilerOptions;

    }

    public class JkRunOptions {

        @JkDoc("JVM options to use when running generated jar")
        @JkDepSuggest(versionOnly = true, hint = "-Xms512m,-Xmx2g,-Xmn128m,-Xss1m,-Xlog:gc,-XX:+UseG1GC," +
                "-XX:+PrintGCDetails,-XX:+HeapDumpOnOutOfMemoryError,-Xdiag,-XshowSettings,-Xlog:exceptions")
        public String jvmOptions = "";

        @JkDoc("Program arguments to use when running generated jar")
        public String programArgs = "";

        @JkDoc("If true, the resolved runbase classpath will be used when running the generated jar. " +
                "If the generated jar is a Uber jar or contains all the needed dependencies, leave it to 'false'")
        public boolean useRuntimeDepsForClasspath;

        void runJar() {
            project.prepareRunJar(JkProject.RuntimeDeps.of(useRuntimeDepsForClasspath))
                    .addJavaOptions(JkUtilsString.parseCommandline(jvmOptions))
                    .addParams(JkUtilsString.parseCommandline(programArgs))
                    .exec();
        }

        void runMain() {
            project.prepareRunMain()
                    .addJavaOptions(JkUtilsString.parseCommandline(jvmOptions))
                    .addParams(JkUtilsString.parseCommandline(programArgs))
                    .exec();
        }
    }

    public static class JkPublishOptions {

        /** When true, javadoc is created and packed in a jar file.*/
        @JkDoc("If true, javadoc jar is added in the list of artifact to produce/publish.")
        public boolean javadoc = true;

        /** When true, sources are packed in a jar file.*/
        @JkDoc("If true, sources jar is added in the list of artifact to produce/publish.")
        public boolean sources = true;

    }

    public class JkProjectScaffoldOptions extends JkScaffoldOptions {

        @JkDoc("Generate libs sub-folders for hosting local libraries")
        private boolean generateLibsFolders = false;

        @JkDoc("The template used for scaffolding the build class")
        private JkProjectScaffold.Kind kind = JkProjectScaffold.Kind.REGULAR;

        public JkProjectScaffold.Kind getKind() {
            return kind;
        }

        @Override
        public void applyTo(JkScaffold scaffold) {
            super.applyTo(scaffold);
            // Scaffold project structure including build class
            JkProjectScaffold projectScaffold = (JkProjectScaffold) scaffold;
            projectScaffold.setTemplate(kind);
            projectScaffold.setUseSimpleStyle(ProjectKBean.this.layout.style == JkCompileLayout.Style.SIMPLE);

            // Create 'project-dependencies.txt' file if needed
            List<String> compileDeps = dependencies.toList(dependencies.compile);
            List<String> runtimeDeps = dependencies.toList(dependencies.runtime);
            List<String> testDeps = dependencies.toList(dependencies.test);
            projectScaffold.compileDeps.addAll(compileDeps);
            projectScaffold.runtimeDeps.addAll(runtimeDeps);
            projectScaffold.testDeps.addAll(testDeps);

            // Create local lib folder structure if needed
            if (generateLibsFolders) {
                projectScaffold.setGenerateLibsFolders(true);
            }
        }
    }

    @Override
    public String toString() {
        return project.toString();
    }

    // ------- private methods

    private JkJavaCompilerToolChain.JkJdks jdks() {
        return JkJavaCompilerToolChain.JkJdks.ofJdkHomeProps(getRunbase().getProperties().getAllStartingWith("jeka.jdk.", false));
    }

    private void applyRepoConfigOn(JkProject project) {

        // set dependency resolver
        JkRepoProperties repoProperties = JkRepoProperties.of(this.getRunbase().getProperties());
        final JkRepoSet downloadRepos = repoProperties.getDownloadRepos();
        if (!downloadRepos.getRepos().isEmpty()) {
            project.dependencyResolver.setRepos(downloadRepos);
        }
    }

    private void configureProject() {
        project.setBaseDir(getBaseDir());
        if (!JkLog.isAnimationAccepted()) {
            project.testing.testProcessor.engineBehavior.setProgressDisplayer(
                    JkTestProcessor.JkProgressStyle.MUTE);
        }
        if (!JkUtilsString.isBlank(version)) {
            project.setVersion(version);
        }
        if (gitVersioning.enable) {
            JkVersionFromGit.handleVersioning(project, gitVersioning.tagPrefix);
        }
        if (!JkUtilsString.isBlank(moduleId)) {
            project.setModuleId(moduleId);
        }
        if (!JkUtilsString.isBlank(sourceEncoding)) {
            project.setSourceEncoding(sourceEncoding);
        }
        project.dependencyResolver.setFileSystemCacheDir(getBaseDir().resolve(JkConstants.JEKA_WORK_PATH)
                .resolve("project-dep-resolution-cache"));
        project.dependencyResolver.setUseFileSystemCache(true);
        if (!JkUtilsString.isBlank(compilation.javaVersion)) {
            JkJavaVersion version = JkJavaVersion.of(compilation.javaVersion);
            project.setJvmTargetVersion(version);
        }
        applyRepoConfigOn(project);
        project.flatFacade.setLayoutStyle(layout.style);
        if (layout.mixSourcesAndResources) {
            project.flatFacade.setMixResourcesAndSources();
        }
        JkJavaCompilerToolChain compilerToolChain = project.compilerToolChain;
        if (!compilerToolChain.isToolOrProcessSpecified()) {
            compilerToolChain.setJdkHints(jdks(), !compilation.fork);
        }
        if (pack.jarType != null) {
            project.flatFacade.setMainArtifactJarType(pack.jarType);
        }
        if (pack.mainClass != null) {
            project.packaging.setMainClass(pack.mainClass);
        }
        project.packaging.setDetectMainClass(pack.detectMainClass);
        if (!JkUtilsString.isBlank(pack.shadeJarClassifier)) {
            project.flatFacade.addShadeJarArtifact(pack.shadeJarClassifier);
        }

        // Configure testing
        JkTestProcessor testProcessor = project.testing.testProcessor;
        testProcessor.setJvmHints(jdks(), project.getJvmTargetVersion());
        if (tests.fork) {
            String className = JkTestProcessor.class.getName();

            JkJavaProcess javaProcess = JkJavaProcess.ofJava(className)
                    .addJavaOptions(this.tests.jvmOptions);
            if (project.getJvmTargetVersion() != null &&
                    !JkJavaVersion.ofCurrent().equals(project.getJvmTargetVersion())) {
                Path javaHome = jdks().getHome(project.getJvmTargetVersion());
                if (javaHome != null) {
                    JkLog.verbose("Tests are configured to run using JDK %s", javaHome);
                    javaProcess.setParamAt(0,javaHome.resolve("bin/java").toString());
                }
            }
            testProcessor.setForkingProcess(javaProcess);
        } else {
            testProcessor.setForkingProcess(false);
        }
        project.testing.setSkipped(tests.skip);

        // -- The style should not be forced by default as it is determined by the presence of a console, and the log level
        if (tests.progress != null) {
            project.testing.testProcessor.engineBehavior.setProgressDisplayer(tests.progress);
        }
        if (!JkUtilsString.isBlank(tests.platformVersion)) {
            project.testing.testProcessor.setJunitPlatformVersion(tests.platformVersion);
        }
        if (compilation.compilerOptions != null) {
            String[] options = JkUtilsString.parseCommandline(compilation.compilerOptions);
            project.compilation.addJavaCompilerOptions(options);
            project.testing.compilation.addJavaCompilerOptions(options);
        }
        List<String> includePatterns = JkUtilsString.splitWhiteSpaces(tests.includePatterns);
        project.testing.testSelection.addIncludePatterns(includePatterns);

        // Configure scaffold
        this.projectScaffold = JkProjectScaffold.of(project);
        this.scaffold.applyTo(projectScaffold); // apply basic configuration from KBean fields
    }

    private static class JkDependenciesOptions {

        @JkDoc("Comma separated compile dependencies to include at scaffold time")
        public String compile;

        @JkDoc("Comma separated runtime dependencies to include at scaffold time")
        public String runtime;

        @JkDoc("Comma separated test dependencies to include at scaffold time")
        public String test;

        List<String> toList(String description) {
            if (description == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(description.split(","));
        }
    }

}
