package dev.jeka.core.api.depmanagement.artifact;

import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This {@link JkArtifactProducer} produces artifacts files at a standardized path
 * determined by a provided function (outputPath, artifactId) -> path. <p/>
 * This function is supposed to be supplied by the caller. To add artifacts to produce, caller has
 * to provide a {@link Consumer<Path>} generating the artifact file at the given path.
 */
public class JkStandardFileArtifactProducer implements JkArtifactProducer {

    private final Map<JkArtifactId, Consumer<Path>> consumers = new LinkedHashMap<>();

    private String mainArtifactExt = "jar";

    private final Supplier<Path> outputDirSupplier;

    private final Supplier<String> baseNameSupplier;

    private JkStandardFileArtifactProducer(Supplier<Path> outputDirSupplier,
                                           Supplier<String> baseNameSupplier) {
        this.outputDirSupplier = outputDirSupplier;
        this.baseNameSupplier = baseNameSupplier;
    }

    public static JkStandardFileArtifactProducer of(Supplier<Path> outputDirSupplier,
                                                    Supplier<String> baseNameSupplier) {
        return new JkStandardFileArtifactProducer(outputDirSupplier, baseNameSupplier);
    }

    @Override
    public void makeArtifact(JkArtifactId artifactId) {
        Consumer<Path> consumer = consumers.get(artifactId);
        if (consumer == null) {
            throw new IllegalArgumentException("No artifact " + artifactId + " defined on this producer. " +
                    "Artifact defined are : " + consumers.entrySet());
        }
        Path path = getArtifactPath(artifactId);
        JkLog.startTask("Making artifact " + path.getFileName());
        consumer.accept(path);
        JkLog.endTask();
    }

    @Override
    public Path getArtifactPath(JkArtifactId artifactId) {
        return outputDirSupplier.get().resolve(artifactId.toFileName(baseNameSupplier.get()));
    }

    @Override
    public List<JkArtifactId> getArtifactIds() {
        return new LinkedList<>(consumers.keySet());
    }

    public JkStandardFileArtifactProducer putArtifact(JkArtifactId artifactId, Consumer<Path> artifactFileMaker) {
        consumers.put(artifactId, artifactFileMaker);
        return this;
    }

    public JkStandardFileArtifactProducer putArtifact(String name, String ext, Consumer<Path> artifactFileMaker) {
        return putArtifact(JkArtifactId.of(name, ext), artifactFileMaker);
    }

    public JkStandardFileArtifactProducer putMainArtifact(Consumer<Path> artifactFileMaker) {
        return putArtifact(getMainArtifactId(), artifactFileMaker);
    }


    public JkStandardFileArtifactProducer removeArtifact(String artifactName) {
        for (JkArtifactId artifactId : consumers.keySet()) {
            if (artifactName.equals(artifactId.getName())) {
                consumers.remove(artifactId);
            }
        }
        return this;
    }

    public JkStandardFileArtifactProducer removeArtifact(JkArtifactId artifactId) {
        consumers.remove(artifactId);
        return this;
    }

    @Override
    public String getMainArtifactExt() {
        return mainArtifactExt;
    }

    public JkStandardFileArtifactProducer setMainArtifactExt(String mainArtifactExt) {
        this.mainArtifactExt = mainArtifactExt;
        return this;
    }



}
