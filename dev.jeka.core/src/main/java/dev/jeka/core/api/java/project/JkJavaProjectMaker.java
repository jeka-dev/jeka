package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkFileSystemLocalizable;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.JkConstants;

import java.nio.file.Path;
import java.util.*;

/**
 * Mainly an artifact producer for a Java project. It embeds also methods for publishing produced artifacts. <p>
 * By default instances of this class include two artifacts : the main artifacts consisting in the binary jar and the source jar.<p>
 * One can include extra artifacts to produce or remove already defined ones. Including new artifacts suppose to provides
 * a {@link Runnable} responsible to actually create
 *
 * All defined tasks are extensible using {@link JkRunnables} mechanism.
 */
public final class JkJavaProjectMaker implements JkArtifactProducer, JkFileSystemLocalizable {

    public static final JkArtifactId SOURCES_ARTIFACT_ID = JkArtifactId.of("sources", "jar");

    public static final JkArtifactId JAVADOC_ARTIFACT_ID = JkArtifactId.of("javadoc", "jar");

    public static final JkArtifactId TEST_ARTIFACT_ID = JkArtifactId.of("test", "jar");

    public static final JkArtifactId TEST_SOURCE_ARTIFACT_ID = JkArtifactId.of("test-sources", "jar");

    final JkJavaProject project;

    private final Map<JkArtifactId, Runnable> artifactRunnables = new LinkedHashMap<>();

    private final Map<Set<JkScope>, JkPathSequence> dependencyCache = new HashMap<>();

    private JkProjectOutLayout outLayout;

    private JkDependencyResolver dependencyResolver;

    private boolean failOnDependencyResolutionError = true;

    private final JkSteps steps;

    private final JkRunnables outputCleaner;

    JkJavaProjectMaker(JkJavaProject project) {
        this.project = project;
        outLayout = JkProjectOutLayout.ofClassicJava().withOutputDir(project.getBaseDir().resolve(
                JkConstants.OUTPUT_PATH));
        outputCleaner = JkRunnables.of(
                () -> JkPathTree.of(getOutLayout().getOutputPath()).deleteContent());
        this.steps = new JkSteps();
        this.steps.init(this);

        // define default artifacts
        putArtifact(getMainArtifactId(), () -> makeMainJar());
        putArtifact(SOURCES_ARTIFACT_ID, () -> makeSourceJar());
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
            this.outLayout = outLayout.withOutputDir(getBaseDir().resolve(outLayout.getOutputPath()));
        }
        return this;
    }

    /**
     * If <code>true</code> this object will throw a JkException whenever a dependency resolution occurs. Otherwise
     * just log a warn message. <code>false</code> by default.
     */
    public JkJavaProjectMaker setFailOnDependencyResolutionError(boolean fail) {
        this.failOnDependencyResolutionError = fail;
        return this;
    }

    // artifact definition -----------------------------------------------------------

    /**
     * Defines how to produce the specified artifact. <br/>
     * The specified artifact can be an already defined artifact (as 'main' artifact), in this
     * case the current definition will be overwritten. <br/>
     * The specified artifact can also be a new artifact (an Uber jar for example or a jar for a specific target platform). <br/>
     * {@link JkJavaProjectMaker} declares predefined artifact ids as {@link JkJavaProjectMaker#SOURCES_ARTIFACT_ID}
     * or {@link JkJavaProjectMaker#JAVADOC_ARTIFACT_ID}.
     */
    public JkJavaProjectMaker putArtifact(JkArtifactId artifactId, Runnable runnable) {
        artifactRunnables.put(artifactId, runnable);
        return this;
    }

    /**
     * Removes the definition of the specified artifacts. Once remove, invoking <code>makeArtifact(theRemovedArtifactId)</code>
     * will raise an exception.
     */
    public JkJavaProjectMaker removeArtifact(JkArtifactId artifactId) {
        artifactRunnables.remove(artifactId);
        return this;
    }

    @Override
    public void makeArtifact(JkArtifactId artifactId) {
        if (artifactRunnables.containsKey(artifactId)) {
            Path resultFile =  project.getBaseDir().relativize(steps.packaging.getArtifactFile(artifactId));
            JkLog.startTask("Making artifact file " + resultFile);
            this.artifactRunnables.get(artifactId).run();
            JkLog.endTask();
            steps.packaging.checksum(steps.packaging.getArtifactFile(artifactId));
        } else {
            throw new IllegalArgumentException("No artifact " + artifactId + " is defined on project " + this.project);
        }
    }

    /**
     * Convenient method for defining a fat jar artifact having the specified classifier name.
     * @param defineOriginal If true, a "original" artifact will be created standing for the original jar.
     */
    public JkJavaProjectMaker defineMainArtifactAsFatJar(boolean defineOriginal) {
        JkArtifactId mainArtifactId = getMainArtifactId();
        Path mainPath = getArtifactPath(mainArtifactId);
        putArtifact(mainArtifactId, () -> {
            steps.compilation.runIfNecessary();
            steps.testing.runIfNecessary();
            steps.packaging.createFatJar(mainPath);});
        if (defineOriginal) {
            JkArtifactId originalArtifactId = JkArtifactId.of("original", "jar");
            putArtifact(originalArtifactId, () -> steps.packaging.createBinJar(getArtifactPath(originalArtifactId)));
        }
        return this;
    }

    @Override
    public Path getArtifactPath(JkArtifactId artifactId) {
        return steps.packaging.getArtifactFile(artifactId);
    }

    @Override
    public final Iterable<JkArtifactId> getArtifactIds() {
        return this.artifactRunnables.keySet();
    }

    public JkJavaProjectMaker addTestArtifact() {
        putArtifact(TEST_ARTIFACT_ID, () -> makeTestJar());
        return this;
    }

    public JkJavaProjectMaker addTestSourceArtifact() {
        putArtifact(TEST_SOURCE_ARTIFACT_ID,
                () -> steps.packaging.createTestSourceJar(steps.packaging.getArtifactFile(TEST_SOURCE_ARTIFACT_ID)));
        return this;
    }

    public JkJavaProjectMaker addJavadocArtifact() {
        putArtifact(JAVADOC_ARTIFACT_ID, () -> makeJavadocJar());
        return this;
    }

    /**
     * Returns the runnable responsible for creating the specified artifactId.
     */
    public Runnable getRunnable(JkArtifactId artifactId) {
        return this.artifactRunnables.get(artifactId);
    }

    // Dependency management -----------------------------------------------------------

    /**
     * Returns lib paths standing for the resolution of this project dependencies for the specified dependency scopes.
     */
    public JkPathSequence fetchDependenciesFor(JkScope... scopes) {
        final Set<JkScope> scopeSet = new HashSet<>(Arrays.asList(scopes));
        return dependencyCache.computeIfAbsent(scopeSet,
                scopes1 -> {
                    JkResolveResult resolveResult =
                            getDependencyResolver().resolve(getScopeDefaultedDependencies(), scopes);
                    JkResolveResult.JkErrorReport report = resolveResult.getErrorReport();
                    if (report.hasErrors()) {
                        if (failOnDependencyResolutionError) {
                            throw new JkException(report.toString());
                        }
                        JkLog.warn(report.toString());
                    }
                    return resolveResult.getFiles();
                });
    }

    /**
     * Returns dependencies declared for this project. Dependencies declared without specifying
     * scope are defaulted to scope {@link JkJavaDepScopes#COMPILE_AND_RUNTIME}
     */
    public JkDependencySet getScopeDefaultedDependencies() {
        return project.getDependencies().withDefaultScopes(JkJavaDepScopes.COMPILE_AND_RUNTIME);
    }

    public JkDependencyResolver getDependencyResolver() {
        if (dependencyResolver == null) {
            dependencyResolver = JkDependencyResolver.of(JkRepo.ofMavenCentral())
                    .withParams(JkResolutionParameters.of(JkJavaDepScopes.DEFAULT_SCOPE_MAPPING))
                    .withBasedir(getBaseDir());
        }
        return dependencyResolver;
    }

    public JkJavaProjectMaker setDependencyResolver(JkDependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver.withBasedir(project.getBaseDir());
        return this;
    }

    /**
     * Shorthand to add a download repository to this project maker.
     */
    public JkJavaProjectMaker addDownloadRepo(JkRepo repo) {
        this.dependencyResolver = this.getDependencyResolver().andRepos(repo.toSet());
        return this;
    }

    public JkJavaProjectMaker setDownloadRepos(JkRepoSet repos) {
        this.dependencyResolver = this.getDependencyResolver().withRepos(repos);
        return this;
    }

    @Override
    public JkPathSequence fetchRuntimeDependencies(JkArtifactId artifactFileId) {
        if (artifactFileId.equals(getMainArtifactId())) {
            return this.getDependencyResolver().resolve(
                    this.project.getDependencies().withDefaultScopes(JkJavaDepScopes.COMPILE_AND_RUNTIME), JkJavaDepScopes.RUNTIME).getFiles();
        } else if (artifactFileId.isClassifier("test") && artifactFileId.isExtension("jar")) {
            return this.getDependencyResolver().resolve(
                    this.project.getDependencies().withDefaultScopes(JkJavaDepScopes.COMPILE_AND_RUNTIME), JkJavaDepScopes.SCOPES_FOR_TEST).getFiles();
        } else {
            return JkPathSequence.of();
        }
    }

    JkJavaProjectMaker cleanDependencyCache() {
        dependencyCache.clear();
        return this;
    }

    // Clean -----------------------------------------------

    /**
     * Holds runnables executed while {@link #clean()} method is invoked. Add your own runnable if you want to
     * improve the <code>clean</code> method.
     */
    public JkRunnables getOutputCleaner() {
        return outputCleaner;
    }

    /**
     * Deletes project build outputs.
     */
    public JkJavaProjectMaker clean() {
        steps.compilation.reset();
        steps.testing.reset();
        steps.documentation.reset();
        outputCleaner.run();
        return this;
    }

    // Phase tasks -----------------------------

    public JkJavaProjectMaker.JkSteps getSteps() {
        return steps;
    }

    // Make -------------------------------------------------------


    private JkJavaProjectMaker makeMainJar() {
        Path target = steps.packaging.getArtifactFile(getMainArtifactId());
        steps.packaging.createBinJar(target);
        return this;
    }

    private JkJavaProjectMaker makeSourceJar() {
        Path target = steps.packaging.getArtifactFile(SOURCES_ARTIFACT_ID);
        steps.packaging.createSourceJar(target);
        return this;
    }

    private JkJavaProjectMaker makeJavadocJar() {
        Path target = steps.packaging.getArtifactFile(JAVADOC_ARTIFACT_ID);
        steps.packaging.createJavadocJar(target);
        return this;
    }

    private JkJavaProjectMaker makeTestJar(Path target) {
        steps.packaging.createTestJar(target);
        return this;
    }

    private JkJavaProjectMaker makeTestJar() {
        makeTestJar(getArtifactPath(TEST_ARTIFACT_ID));
        return this;
    }

    @Override
    public String toString() {
        return this.project.toString();
    }

    public static class JkSteps {

        private JkJavaProjectMakerCompilationStep.JkProduction compilation;

        private JkJavaProjectMakerTestingStep testing;

        private JkJavaProjectMakerPackagingStep packaging;

        private JkJavaProjectMakerPublishingStep publishing;

        private JkJavaProjectMakerDocumentationStep documentation;

        private JkSteps() {
        }

        private void init(JkJavaProjectMaker maker) {
            compilation = JkJavaProjectMakerCompilationStep.JkProduction.of(maker);
            testing = JkJavaProjectMakerTestingStep.of(maker);
            packaging = JkJavaProjectMakerPackagingStep.of(maker);
            publishing = new JkJavaProjectMakerPublishingStep(maker);
            documentation = JkJavaProjectMakerDocumentationStep.of(maker);
        }

        public JkJavaProjectMakerCompilationStep.JkProduction getCompilation() {
            return compilation;
        }

        public JkJavaProjectMakerTestingStep getTesting() {
            return testing;
        }

        public JkJavaProjectMakerPackagingStep getPackaging() {
            return packaging;
        }

        public JkJavaProjectMakerPublishingStep getPublishing() {
            return publishing;
        }

        public JkJavaProjectMakerDocumentationStep getDocumentation() {
            return documentation;
        }
    }


}
