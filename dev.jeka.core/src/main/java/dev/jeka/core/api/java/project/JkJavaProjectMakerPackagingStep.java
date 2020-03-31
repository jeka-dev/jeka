package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkArtifactId;
import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClasspath;
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
public class JkJavaProjectMakerPackagingStep {

    private final JkJavaProjectMaker maker;

    private Supplier<String> artifactFileNameSupplier;

    private String[] checksumAlgorithms = new String[0];

    private PathMatcher fatJarFilter = JkPathMatcher.of(); // take all

    private JkManifest manifest;

    /**
     * For Parent chaining
     */
    public JkJavaProjectMaker.JkSteps _;

    private JkJavaProjectMakerPackagingStep(JkJavaProjectMaker maker) {
        this.maker = maker;
        this._ = maker.getSteps();
        artifactFileNameSupplier = getModuleNameFileNameSupplier();
    }

    static JkJavaProjectMakerPackagingStep of(JkJavaProjectMaker maker) {
        JkJavaProjectMakerPackagingStep result = new JkJavaProjectMakerPackagingStep(maker);
        result.manifest = JkManifest.of(result);
        return result;
    }

    public JkManifest<JkJavaProjectMakerPackagingStep> getManifest() {
        return manifest;
    }

    /**
     * Returns an artifact file name supplier for including version in artifact file names.
     */
    public Supplier<String> getIncludingVersionFileNameSupplier() {
        JkVersionedModule module = maker.project.getVersionedModule();
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
        return () -> maker.project.getVersionedModule().getModuleId().getDotedName();
    }

    Path getArtifactFile(JkArtifactId artifactId) {
        final String namePart = artifactFileNameSupplier.get();
        final String classifier = artifactId.getClassifier() == null ? "" : "-" + artifactId.getClassifier();
        final String extension = artifactId.getExtension() == null ? "" : "." + artifactId.getExtension();
        return maker.getOutLayout().getOutputPath().resolve(namePart + classifier + extension);
    }

    public Supplier<String> getArtifactFileNameSupplier() {
        return artifactFileNameSupplier;
    }

    public void createBinJar(Path target) {
        maker.getSteps().getCompilation().runIfNecessary();
        maker.getSteps().getTesting().runIfNecessary();
        JkJavaProject project = maker.project;
        JkJarPacker.of(maker.getOutLayout().getClassDir())
                .withManifest(manifest)
                .withExtraFiles(project.getExtraFilesToIncludeInJar())
                .makeJar(target);
    }


    public void createFatJar(Path target) {
        maker.getSteps().getCompilation().runIfNecessary();
        maker.getSteps().getTesting().runIfNecessary();
        JkClasspath classpath = JkClasspath.of(maker.fetchRuntimeDependencies(maker.getMainArtifactId()));
        JkJarPacker.of( maker.getOutLayout().getClassDir())
                .withManifest(manifest)
                .withExtraFiles(maker.project.getExtraFilesToIncludeInJar())
                .makeFatJar(target, classpath, this.fatJarFilter);
    }

    public void createSourceJar(Path target) {
        maker.project.getSourceLayout().getSources().and(maker.getOutLayout().getGeneratedSourceDir()).zipTo(target);
    }

    void createJavadocJar(Path target) {
        maker.getSteps().getDocumentation().runIfNecessary();
        Path javadocDir = maker.getOutLayout().getJavadocDir();
        if (!Files.exists(javadocDir)) {
            throw new IllegalStateException("No javadoc has not been generated in " + javadocDir.toAbsolutePath()
                    + ". Can't create a javadoc jar until javadoc files has been generated.");
        }
        JkPathTree.of(javadocDir).zipTo(target);
    }

    public void createTestJar(Path target) {
        maker.getSteps().getCompilation().runIfNecessary();
        maker.getSteps().getTesting().runIfNecessary();
        JkJarPacker.of(maker.getOutLayout().getTestClassDir())
                .withManifest(manifest)
                .makeJar(target);
    }

    void createTestSourceJar(Path target) {
        maker.project.getSourceLayout().getTests().zipTo(target);
    }

    /**
     * Specifies how the name of the artifact files will be constructed.
     * Given artifact file name are always structured as XXXXX-classifier.ext,
     * this method acts on the XXXXX part.
     */
    public JkJavaProjectMakerPackagingStep setArtifactFileNameSupplier(Supplier<String> artifactFileNameSupplier) {
        this.artifactFileNameSupplier = artifactFileNameSupplier;
        return this;
    }

    /**
     * Defines the algorithms to sign the produced artifacts.
     * @param algorithms Digest algorithm working on JDK8 platform including <code>md5, sha-1, sha-2 and sha-256</code>
     */
    public JkJavaProjectMakerPackagingStep setChecksumAlgorithms(String ... algorithms) {
        this.checksumAlgorithms = algorithms;
        return this;
    }

    /**
     * Defines witch files from main jar and dependency jars will be included in the fat jar.
     * By default, it is valued to "all".
     */
    public JkJavaProjectMakerPackagingStep setFatJarFilter(PathMatcher fatJarFilter) {
        JkUtilsAssert.notNull(fatJarFilter, "Fat jar filter can not be null.");
        this.fatJarFilter = fatJarFilter;
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
