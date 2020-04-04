package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkArtifactId;
import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJarPacker;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.function.Supplier;

/**
 * Tasks for packaging artifacts created by the holding project.
 */
public class JkJavaProjectPackaging {

    private final JkJavaProject project;

    private Supplier<String> artifactFileNameSupplier;

    private String[] checksumAlgorithms = new String[0];

    private PathMatcher fatJarFilter = JkPathMatcher.of(); // take all

    private final JkManifest manifest;

    private JkPathTreeSet extraFilesToIncludeInFatJar = JkPathTreeSet.ofEmpty();

    private final JkJavaProjectCompilation compilationStep;

    private final JkJavaProjectDocumentation documentationStep;

    /**
     * For Parent chaining
     */
    public JkJavaProject.JkSteps __;

    JkJavaProjectPackaging(JkJavaProject project, JkJavaProject.JkSteps steps) {
        this.project = project;
        this.__ = steps;
        artifactFileNameSupplier = getModuleNameFileNameSupplier();
        manifest = JkManifest.ofParent(this);
        compilationStep = steps.getCompilation();
        documentationStep = steps.getDocumentation();
    }

    public JkManifest<JkJavaProjectPackaging> getManifest() {
        return manifest;
    }

    /**
     * Returns an artifact file name supplier for including version in artifact file names.
     */
    public Supplier<String> getIncludingVersionFileNameSupplier() {
        JkVersionedModule module = defaultVersionedModule();
        return () -> {
            String version = module.getVersion().isUnspecified() ? "" : "-"
                    + module.getVersion().getValue();
            return module.getModuleId().getDotedName() + version;
        };
    }

    /**
     * Returns an artifact file name supplier for NOT including version in artifact file names.
     */
    public Supplier<String> getModuleNameFileNameSupplier() {
        return () -> defaultVersionedModule().getModuleId().getDotedName();
    }

    private JkVersionedModule defaultVersionedModule() {
        JkVersionedModule versionedModule = project.getSteps().getPublishing().getVersionedModule();
        if (versionedModule == null) {
            return JkVersionedModule.ofRootDirName(project.getBaseDir().getFileName().toString());
        }
        return versionedModule;
    }

    Path getArtifactFile(JkArtifactId artifactId) {
        final String namePart = artifactFileNameSupplier.get();
        final String classifier = artifactId.getClassifier() == null ? "" : "-" + artifactId.getClassifier();
        final String extension = artifactId.getExtension() == null ? "" : "." + artifactId.getExtension();
        return project.getOutputDir().resolve(namePart + classifier + extension);
    }

    public Supplier<String> getArtifactFileNameSupplier() {
        return artifactFileNameSupplier;
    }

    public void createBinJar(Path target) {
        project.getSteps().getCompilation().runIfNecessary();
        project.getSteps().getTesting().runIfNecessary();
        JkJarPacker.of(compilationStep.getLayout().resolveClassDir())
                .withManifest(manifest)
                .withExtraFiles(getExtraFilesToIncludeInJar())
                .makeJar(target);
    }

    public void createFatJar(Path target) {
        project.getSteps().getCompilation().runIfNecessary();
        project.getSteps().getTesting().runIfNecessary();
        Iterable<Path> classpath = project.getDependencyManagement()
                .fetchDependencies(JkJavaDepScopes.RUNTIME).getFiles();
        JkJarPacker.of(compilationStep.getLayout().resolveClassDir())
                .withManifest(manifest)
                .withExtraFiles(getExtraFilesToIncludeInJar())
                .makeFatJar(target, classpath, this.fatJarFilter);
    }

    public void createSourceJar(Path target) {
        compilationStep.getLayout().resolveSources().and(compilationStep
                .getLayout().resolveGeneratedSourceDir()).zipTo(target);
    }

    void createJavadocJar(Path target) {
        project.getSteps().getDocumentation().runIfNecessary();
        Path javadocDir = documentationStep.getJavadocDir();
        if (!Files.exists(javadocDir)) {
            throw new IllegalStateException("No javadoc has not been generated in " + javadocDir.toAbsolutePath()
                    + ". Can't create a javadoc jar until javadoc files has been generated.");
        }
        JkPathTree.of(javadocDir).zipTo(target);
    }

    /**
     * Defines the algorithms to sign the produced artifacts.
     * @param algorithms Digest algorithm working on JDK8 platform including <code>md5, sha-1, sha-2 and sha-256</code>
     */
    public JkJavaProjectPackaging setChecksumAlgorithms(String ... algorithms) {
        this.checksumAlgorithms = algorithms;
        return this;
    }

    /**
     * Defines witch files from main jar and dependency jars will be included in the fat jar.
     * By default, it is valued to "all".
     */
    public JkJavaProjectPackaging setFatJarFilter(PathMatcher fatJarFilter) {
        JkUtilsAssert.notNull(fatJarFilter, "Fat jar filter can not be null.");
        this.fatJarFilter = fatJarFilter;
        return this;
    }

    public JkPathTreeSet getExtraFilesToIncludeInJar() {
        return this.extraFilesToIncludeInFatJar;
    }

    /**
     * File trees specified here will be added to the fat jar.
     */
    public JkJavaProjectPackaging setExtraFilesToIncludeInFatJar(JkPathTreeSet extraFilesToIncludeInFatJar) {
        this.extraFilesToIncludeInFatJar = extraFilesToIncludeInFatJar;
        return this;
    }

    /**
     * Creates a checksum file of each specified digest algorithm for the specified file.
     * Checksum files will be created in same folder as their respecting artifact files with the same name suffixed
     * by '.' and the name of the checksumm algorithm. <br/>.
     */
    void checksum(Path fileToChecksum) {
        for (String algo : checksumAlgorithms) {
            JkLog.startTask("Creating checksum " + algo + " for file " + fileToChecksum);
            JkPathFile.of(fileToChecksum).checksum(algo);
            JkLog.endTask();
        }

    }

}
