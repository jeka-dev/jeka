package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkArtifactId;
import dev.jeka.core.api.depmanagement.JkArtifactProducer;
import dev.jeka.core.api.depmanagement.JkDependencyManagement;
import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.file.JkFileSystemLocalizable;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mainly an artifact producer for a Java project. It embeds also methods for publishing produced artifacts. <p>
 * By default instances of this class include two artifacts : the main artifacts consisting in the binary jar and the source jar.<p>
 * One can include extra artifacts to produce or remove already defined ones. Including new artifacts suppose to provides
 * a {@link Runnable} responsible to actually create
 *
 * All defined tasks are extensible using {@link JkRunnables} mechanism.
 */
final class JkJavaProjectMaker implements JkArtifactProducer, JkFileSystemLocalizable {

    public static final JkArtifactId SOURCES_ARTIFACT_ID = JkArtifactId.of("sources", "jar");

    public static final JkArtifactId JAVADOC_ARTIFACT_ID = JkArtifactId.of("javadoc", "jar");

    public static final JkArtifactId TEST_ARTIFACT_ID = JkArtifactId.of("test", "jar");

    public static final JkArtifactId TEST_SOURCE_ARTIFACT_ID = JkArtifactId.of("test-sources", "jar");

    final JkJavaProject project;

    private final Map<JkArtifactId, Runnable> artifactRunnables = new LinkedHashMap<>();

    private final JkSteps steps;

    JkJavaProjectMaker(JkJavaProject project) {
        this.project = project;

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
     * Shorthand for <code>putArtifact(this.getMainArtifact, runnable)</code>.
     */
    public JkJavaProjectMaker setMainArtifact(Runnable runnable) {
        artifactRunnables.put(getMainArtifactId(), runnable);
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


    @Override
    public JkPathSequence fetchRuntimeDependencies(JkArtifactId artifactFileId) {
        JkDependencyManagement dm = project.getDependencyManagement();
        if (artifactFileId.equals(getMainArtifactId())) {
            return dm.fetchDependencies(JkJavaDepScopes.RUNTIME).getFiles();
        } else if (artifactFileId.isClassifier("test") && artifactFileId.isExtension("jar")) {
            return dm.fetchDependencies(JkJavaDepScopes.SCOPES_FOR_TEST).getFiles();
        } else {
            return JkPathSequence.of();
        }
    }

    // Clean -----------------------------------------------

    /**
     * Deletes project build outputs.
     */
    public JkJavaProjectMaker reset() {
        steps.compilation.reset();
        steps.testing.reset();
        steps.documentation.reset();
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

        private JkJavaProjectMakerCompilationStep compilation;

        private JkJavaProjectMakerTestingStep testing;

        private JkJavaProjectMakerPackagingStep packaging;

        private JkJavaProjectMakerPublishingStep publishing;

        private JkJavaProjectMakerDocumentationStep documentation;

        private JkSteps() {
        }

        // Members should not be instantiated in constructor to avoid maker = null
        private void init(JkJavaProjectMaker maker) {/*
            compilation = JkJavaProjectMakerCompilationStep.ofProd(maker);
            testing = new JkJavaProjectMakerTestingStep(maker);
            packaging = JkJavaProjectMakerPackagingStep.of(maker);
            publishing = new JkJavaProjectMakerPublishingStep(maker);
            documentation = JkJavaProjectMakerDocumentationStep.of(maker);
            */
        }

        public JkJavaProjectMakerCompilationStep<JkSteps> getCompilation() {
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
