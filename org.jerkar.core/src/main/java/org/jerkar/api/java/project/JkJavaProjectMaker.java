package org.jerkar.api.java.project;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkFileSystemLocalizable;
import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.function.JkRunnables;
import org.jerkar.api.system.JkLog;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Objects responsible to build (to make) a java project. It provides methods to perform common build
 * tasks (compile, test, javadoc, package jars, publish artifacts) along methods to define how to build extra artifacts.
 *
 * All defined tasks are extensible using {@link JkRunnables} mechanism.
 */
public final class JkJavaProjectMaker implements JkArtifactProducer, JkFileSystemLocalizable {

    public static final JkArtifactId SOURCES_ARTIFACT_ID = JkArtifactId.of("sources", "jar");

    public static final JkArtifactId JAVADOC_ARTIFACT_ID = JkArtifactId.of("javadoc", "jar");

    public static final JkArtifactId TEST_ARTIFACT_ID = JkArtifactId.of("test", "jar");

    public static final JkArtifactId TEST_SOURCE_ARTIFACT_ID = JkArtifactId.of("test-sources", "jar");

    final JkJavaProject project;

    private boolean skipTests = false;

    private final Status status = new Status();

    private final Map<JkArtifactId, Runnable> artifactProducers = new LinkedHashMap<>();


    private final Map<Set<JkScope>, JkPathSequence> dependencyCache = new HashMap<>();

    private JkProjectOutLayout outLayout;

    private JkDependencyResolver dependencyResolver;

    private final JkJavaProjectCompileTasks compileTasks;

    private final JkJavaProjectTestTasks testTasks;

    private final JkJavaProjectPackTasks packTasks;

    private final JkJavaProjectPublishTasks publishTasks;

    private final JkJavaProjectJavadocTasks javadocTasks;

    private final JkRunnables cleaner;

    JkJavaProjectMaker(JkJavaProject project) {
        outLayout = JkProjectOutLayout.ofClassicJava().withOutputDir(project.getBaseDir().resolve("jerkar/output"));

        cleaner = JkRunnables.of(
                () -> JkPathTree.of(getOutLayout().getOutputPath()).deleteContent());
        final Charset charset = project.getCompileSpec().getEncoding() == null ? Charset.defaultCharset() :
                Charset.forName(project.getCompileSpec().getEncoding());
        compileTasks = new JkJavaProjectCompileTasks(this, charset);
        testTasks = new JkJavaProjectTestTasks(this, charset);
        packTasks = new JkJavaProjectPackTasks(this);
        publishTasks = new JkJavaProjectPublishTasks(this);
        javadocTasks = new JkJavaProjectJavadocTasks(this);

        // define default artifacts
        defineArtifact(getMainArtifactId(), () -> makeMainJar());
        defineArtifact(SOURCES_ARTIFACT_ID, () -> makeSourceJar());
        this.project = project;
    }

    @Override
    public Path getBaseDir() {
        return this.project.getBaseDir();
    }

    public JkProjectOutLayout getOutLayout() {
        return outLayout;
    }

    public JkJavaProjectMaker setOutLayout(JkProjectOutLayout outLayout) {
        if (outLayout.getOutputPath().isAbsolute()) {
            this.outLayout = outLayout;
        } else {
            this.outLayout = outLayout.withOutputDir(this.project.getBaseDir().resolve(outLayout.getOutputPath()));
        }
        return this;
    }

    // artifact definition -----------------------------------------------------------

    /**
     * Defines how to produce the specified artifact. <br/>
     * The specified artifact can be an already defined artifact (as 'main' artifact), in this
     * case the current definition will be overwritten. <br/>
     * The specified artifact can also be a new artifact (as an Uber jar for example). <br/>
     * {@link JkJavaProjectMaker} declares predefined artifact ids as {@link JkJavaProjectMaker#SOURCES_ARTIFACT_ID}
     * or {@link JkJavaProjectMaker#JAVADOC_ARTIFACT_ID}.
     */
    public JkJavaProjectMaker defineArtifact(JkArtifactId artifactId, Runnable runnable) {
        artifactProducers.put(artifactId, runnable);
        return this;
    }

    /**
     * Removes the definition of the specified artifacts. Once remove, invoking <code>makeArtifact(theRemovedArtifactId)</code>
     * will raise an exception.
     */
    public JkJavaProjectMaker undefineArtifact(JkArtifactId artifactId) {
        artifactProducers.remove(artifactId);
        return this;
    }

    @Override
    public void makeArtifact(JkArtifactId artifactId) {
        if (artifactProducers.containsKey(artifactId)) {
            Path resultFile =  project.getBaseDir().relativize(packTasks.getArtifactFile(artifactId));
            JkLog.startTask("Producing artifact file " + resultFile);
            this.artifactProducers.get(artifactId).run();
            JkLog.endTask();
            this.getPackTasks().checksum(resultFile);
        } else {
            throw new IllegalArgumentException("No artifact " + artifactId + " is defined on project " + this.project);
        }
    }

    /**
     * Convenient method for defining a fat jar artifact having the specified classifier name.
     * @param defineOriginal If true, a "original" artifact will be created standing for the original jar.
     */
    public JkJavaProjectMaker defineMainArtifactAsFatJar(boolean defineOriginal) {
        Path mainPath = getArtifactPath(getMainArtifactId());
        Runnable originalRun = artifactProducers.get(getMainArtifactId());
        defineArtifact(getMainArtifactId(), () -> {compileAndTestIfNeeded(); packTasks.createFatJar(mainPath);});
        if (defineOriginal) {
            JkArtifactId original = JkArtifactId.of("original", "jar");
            defineArtifact(original, originalRun);
        }
        return this;
    }

    @Override
    public Path getArtifactPath(JkArtifactId artifactId) {
        return packTasks.getArtifactFile(artifactId);
    }

    @Override
    public final Iterable<JkArtifactId> getArtifactIds() {
        return this.artifactProducers.keySet();
    }

    public void defineTestArtifact() {
        defineArtifact(TEST_ARTIFACT_ID, () -> makeTestJar());
    }

    public void defineTestSourceArtifact() {
        defineArtifact(TEST_SOURCE_ARTIFACT_ID,
                () -> packTasks.createTestSourceJar(packTasks.getArtifactFile(TEST_SOURCE_ARTIFACT_ID)));
    }

    public void defineJavadocArtifact() {
        defineArtifact(JAVADOC_ARTIFACT_ID, () -> makeJavadocJar());
    }

    /**
     * Returns the runnable responsible for creating the specified artifactId.
     */
    public Runnable getRunnable(JkArtifactId artifactId) {
        return this.artifactProducers.get(artifactId);
    }

    // Dependency management -----------------------------------------------------------

    /**
     * Returns lib paths standing for the resolution of this project dependencies for the specified dependency scopes.
     */
    public JkPathSequence fetchDependenciesFor(JkScope... scopes) {
        final Set<JkScope> scopeSet = new HashSet<>(Arrays.asList(scopes));
        return dependencyCache.computeIfAbsent(scopeSet,
                scopes1 -> dependencyResolver().fetch(getDefaultedDependencies(), scopes));
    }

    /**
     * Returns dependencies declared for this project. Dependencies declared without specifying
     * scope are defaulted to scope {@link JkJavaDepScopes#COMPILE_AND_RUNTIME}
     */
    public JkDependencySet getDefaultedDependencies() {
        return project.getDependencies().withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME);
    }

    private JkDependencyResolver dependencyResolver() {
        if (dependencyResolver == null) {
            dependencyResolver = JkDependencyResolver.of(JkRepo.ofMavenCentral())
                  .withParams(JkResolutionParameters.of(JkJavaDepScopes.DEFAULT_SCOPE_MAPPING));
        }
        return dependencyResolver;
    }

    public JkDependencyResolver getDependencyResolver() {
        return dependencyResolver();
    }

    public JkJavaProjectMaker setDependencyResolver(JkDependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
        return this;
    }

    public JkJavaProjectMaker setDownloadRepos(JkRepoSet repos) {
        this.dependencyResolver = this.dependencyResolver().withRepos(repos);
        return this;
    }

    @Override
    public JkPathSequence fetchRuntimeDependencies(JkArtifactId artifactFileId) {
        if (artifactFileId.equals(getMainArtifactId())) {
            return this.getDependencyResolver().fetch(
                    this.project.getDependencies().withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME), JkJavaDepScopes.RUNTIME);
        } else if (artifactFileId.isClassifier("test") && artifactFileId.isExtension("jar")) {
            return this.getDependencyResolver().fetch(
                    this.project.getDependencies().withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME), JkJavaDepScopes.SCOPES_FOR_TEST);
        } else {
            return JkPathSequence.of();
        }
    }

    void cleanDependencyCache() {
        dependencyCache.clear();
    }

    // Clean -----------------------------------------------

    /**
     * Holds runnables executed while {@link #clean()} method is invoked. Add your own runnable if you want to
     * improve the <code>clean</code> method.
     */
    public JkRunnables getCleaner() {
        return cleaner;
    }

    /**
     * Deletes project build outputs.
     */
    public JkJavaProjectMaker clean() {
        status.reset();
        cleaner.run();
        return this;
    }

    // Phase tasks -----------------------------

    public JkJavaProjectCompileTasks getCompileTasks() {
        return compileTasks;
    }

    public JkJavaProjectTestTasks getTestTasks() {
        return testTasks;
    }

    public JkJavaProjectPackTasks getPackTasks() {
        return packTasks;
    }

    public JkJavaProjectPublishTasks getPublishTasks() {
        return publishTasks;
    }


    // Make -------------------------------------------------------

    /**
     * Performs the whole compilation phase including : <ul>
     * <li>Generating resources</li>
     * <li>Generating sources</li>
     * <li>Processing resources (interpolation)</li>
     * <li>Compiling sources</li>
     * </ul>
     */
    public JkJavaProjectMaker compile() {
        JkLog.startTask("Compilation and resource processing");
        compileTasks.run();
        this.status.compileDone = true;
        JkLog.endTask();
        return this;
    }

    public JkJavaProjectMaker test() {
        JkLog.startTask("Running unit tests");
        if (this.project.getSourceLayout().getTests().count(0, false) == 0) {
            JkLog.info("No unit test found in : " + project.getSourceLayout().getTests());
            JkLog.endTask();
            return this;
        }
        if (!this.status.compileOutputPresent()) {
            compile();
        }
        testTasks.run();
        status.testDone = true;
        JkLog.endTask();
        return this;
    }

    private void compileAndTestIfNeeded() {
        if (!status.compileDone) {
            compile();
        }
        if (!skipTests && !status.testDone) {
            test();
        }
    }

    private void makeMainJar() {
        compileAndTestIfNeeded();
        Path target = packTasks.getArtifactFile(getMainArtifactId());
        packTasks.createJar(target);
    }

    private void makeSourceJar() {
        if (!status.sourceGenerationDone) {
            compileTasks.getSourceGenerator().run();
            status.sourceGenerationDone = true;
        }
        Path target = packTasks.getArtifactFile(SOURCES_ARTIFACT_ID);
        packTasks.createSourceJar(target);
    }

    private void generateJavadoc() {
        javadocTasks.run();
        status.javadocDone = true;
    }

    private void makeJavadocJar() {
        if (!status.javadocDone) {
            generateJavadoc();
        }
        Path target = packTasks.getArtifactFile(JAVADOC_ARTIFACT_ID);
        packTasks.createJavadocJar(target);
    }

    private void makeTestJar(Path target) {
        compileAndTestIfNeeded();
        if (!status.testDone) {
            test();
        }
        packTasks.createTestJar(target);
    }

    private void makeTestJar() {
        makeTestJar(getArtifactPath(TEST_ARTIFACT_ID));
    }

    public boolean isTestSkipped() {
        return skipTests;
    }

    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    @Override
    public String toString() {
        return this.project.toString();
    }

    private class Status {

        private boolean sourceGenerationDone = false;

        private boolean compileDone = false;

        private boolean testDone = false;

        private boolean javadocDone = false;

        void reset() {
            sourceGenerationDone = false;
            compileDone = false;
            testDone = false;
            javadocDone = false;
        }

        boolean compileOutputPresent() {
            return Files.exists(JkJavaProjectMaker.this.getOutLayout().getClassDir());
        }

    }
}
