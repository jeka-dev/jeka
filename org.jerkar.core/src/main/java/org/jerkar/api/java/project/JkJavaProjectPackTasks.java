package org.jerkar.api.java.project;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.depmanagement.JkArtifactId;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkPathFile;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.function.JkRunnables;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJarMaker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class JkJavaProjectPackTasks {

    private final JkJavaProjectMaker maker;

    private Supplier<String> artifactFileNameSupplier;

    // Known working algorithm working on JDK8 platform includes <code>md5, sha-1, sha-2 and sha-256</code>
    private String[] digestAlgorithms = new String[0];

    private boolean signArtifacts;

    private final JkRunnables postActions = JkRunnables.of(() -> {});

    JkJavaProjectPackTasks(JkJavaProjectMaker maker) {
        this.maker = maker;
        artifactFileNameSupplier = getModuleNameFileNameSupplier();
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
        return maker.getOutLayout().outputPath().resolve(namePart + classifier + extension);
    }

    public Supplier<String> getArtifactFileNameSupplier() {
        return artifactFileNameSupplier;
    }

    void createJar(Path target) {
        JkJavaProject project = maker.project;
        JkJarMaker.of(maker.getOutLayout().classDir())
                .withManifest(project.getManifest())
                .withExtraFiles(project.getExtraFilesToIncludeInJar())
                .makeJar(target);
    }


    void createFatJar(Path target) {
        JkClasspath classpath = JkClasspath.ofMany(maker.fetchRuntimeDependencies(maker.getMainArtifactId()));
        JkJarMaker.of( maker.getOutLayout().classDir())
                .withManifest(maker.project.getManifest())
                .withExtraFiles(maker.project.getExtraFilesToIncludeInJar())
                .makeFatJar(target, classpath);
    }

    void createSourceJar(Path target) {
        maker.project.getSourceLayout().sources().and(maker.getOutLayout().generatedSourceDir()).zipTo(target);
    }

    void createJavadocJar(Path target) {
        Path javadocDir = maker.getOutLayout().getJavadocDir();
        if (!Files.exists(javadocDir)) {
            throw new IllegalStateException("No javadoc has not been generated in " + javadocDir.toAbsolutePath()
                    + ". Can't create a javadoc jar until javadoc files has been generated.");
        }
        JkPathTree.of(javadocDir).zipTo(target);
    }

    void createTestJar(Path target) {
        JkJarMaker.of(maker.getOutLayout().testClassDir())
                .withManifest(maker.project.getManifest())
                .makeJar(target);
    }

    void createTestSourceJar(Path target) {
        maker.project.getSourceLayout().tests().zipTo(target);
    }

    public JkJavaProjectPackTasks setArtifactFileNameSupplier(Supplier<String> artifactFileNameSupplier) {
        this.artifactFileNameSupplier = artifactFileNameSupplier;
        return this;
    }

    /**
     * Defines the algorithms to sign the produced artifacts.
     * @param algorithms Digest algorithm working on JDK8 platform including <code>md5, sha-1, sha-2 and sha-256</code>
     */
    public JkJavaProjectPackTasks setDigestAlgorithms(String ... algorithms) {
        this.digestAlgorithms = algorithms;
        return this;
    }

    public JkJavaProjectPackTasks setSignArtifacts(boolean signArtifacts) {
        this.signArtifacts = signArtifacts;
        return this;
    }

    /**
     * Chain of actions that will be executed at the end of {@link JkJavaProjectMaker#pack(Iterable)} method.
     */
    public final JkRunnables getPostActions() {
        return postActions;
    }

    /**
     * Creates a checksum file of each specified digest algorithm and each existing defined artifact file.
     * Checksum files will be created in same folder as their respecting artifact files with the same name suffixed
     * by '.' and the name of the checksumm algorithm. <br/>
     * Known working algorithm working on JDK8 platform includes <code>md5, sha-1, sha-2 and sha-256</code>.
     */
    void checksum() {
        maker.getAllArtifactPaths().stream().filter(Files::exists)
                .forEach((file) -> JkPathFile.of(file).checksum(digestAlgorithms));
    }


}
