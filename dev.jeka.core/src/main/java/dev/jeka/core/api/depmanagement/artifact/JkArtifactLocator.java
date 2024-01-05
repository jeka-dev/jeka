package dev.jeka.core.api.depmanagement.artifact;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Defines methods for enumerating artifacts files likely to be produced.
 */
public class JkArtifactLocator {

    private final Supplier<Path> outputDirSupplier;

    private final Supplier<String> baseNameSupplier;

    private JkArtifactLocator(Supplier<Path> outputDirSupplier, Supplier<String> baseNameSupplier) {
        this.outputDirSupplier = outputDirSupplier;
        this.baseNameSupplier = baseNameSupplier;
    }

    public static JkArtifactLocator of(Supplier<Path> outputDirSupplier,
                                        Supplier<String> baseNameSupplier) {
        return new JkArtifactLocator(outputDirSupplier, baseNameSupplier);
    }

    public static JkArtifactLocator of(Path outputDir, String artifactBaseName) {
        return of(() ->outputDir, () -> artifactBaseName);
    }

    /**
     * Returns file system path where is supposed to be produced the specified artifact file id. This method is supposed
     * to only returns the file reference and not generate it.
     */
    public Path getArtifactPath(JkArtifactId artifactId) {
        return outputDirSupplier.get().resolve(artifactId.toFileName(baseNameSupplier.get()));
    }

    public Path getArtifactPath(String qualifier, String extension) {
        return getArtifactPath(JkArtifactId.of(qualifier, extension));
    }

    /**
     * Returns the main artifact file id for this producer. By default, it returns an artifact file id with no
     * classifier and 'jar' getExtension.
     */
    public JkArtifactId getMainArtifactId() {
        return JkArtifactId.of(JkArtifactId.MAIN_ARTIFACT_CLASSIFIER, getMainArtifactExt());
    }

    /**
     * Returns the extension used by the main artifact.
     */
    public String getMainArtifactExt() {
        return "jar";
    }

    /**
     * Returns the main artifact path.
     */
    public Path getMainArtifactPath() {
        return getArtifactPath(getMainArtifactId());
    }



}
