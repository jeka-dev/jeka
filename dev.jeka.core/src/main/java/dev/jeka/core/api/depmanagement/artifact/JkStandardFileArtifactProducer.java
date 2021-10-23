package dev.jeka.core.api.depmanagement.artifact;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This {@link JkArtifactProducer} produces artifacts files at a standardized path
 * determined by a provided function (outputPath, artifactId) -> path. <p/>
 * This function is supposed to be supplied by the caller. To add artifacts to produce, caller has
 * to provide a {@link Consumer<Path>} generating the artifact file at the given path.
 */
public class JkStandardFileArtifactProducer<T> implements JkArtifactProducer {

    /**
     * For parent chaining
     */
    public final T __;

    private final Map<JkArtifactId, Consumer<Path>> consumers = new LinkedHashMap<>();

    private Function<JkArtifactId, Path> artifactFileFunction;

    private String mainArtifactExt = "jar";

    private JkStandardFileArtifactProducer(T __) {
        this.__ = __;
    }

    public static <T> JkStandardFileArtifactProducer<T> ofParent(T __) {
        return new JkStandardFileArtifactProducer<>( __);
    }

    public static JkStandardFileArtifactProducer<Void> of(Function<JkArtifactId, Path> artifactPathFunction) {
        return new JkStandardFileArtifactProducer(null).setArtifactFilenameComputation(artifactPathFunction);
    }

    @Override
    public void makeArtifact(JkArtifactId artifactId) {
        Consumer<Path> consumer = consumers.get(artifactId);
        if (consumer == null) {
            throw new IllegalArgumentException("No artifact " + artifactId + " defined on this producer. " +
                    "Artifact defined are : " + consumers.entrySet());
        }
        Path path = getArtifactPath(artifactId);
        JkLog.startTask("Make artifact " + path.getFileName());
        consumer.accept(path);
        JkLog.info("Artifact created at " + JkUtilsPath.relativizeFromWorkingDir(path));
        JkLog.endTask();
    }

    @Override
    public Path getArtifactPath(JkArtifactId artifactId) {
        JkUtilsAssert.state(artifactFileFunction != null, "artifactFileFunction has not been set.");
        return artifactFileFunction.apply(artifactId);
    }

    @Override
    public List<JkArtifactId> getArtifactIds() {
        return new LinkedList<>(consumers.keySet());
    }

    /**
     * Specifies how the location and names or artifact files will be computed.
     * Artifact files are generated on a given directory provided by the specified supplier. The name of the
     * artifact files will be composed as [partName](-[artifactId.name]).[artifactId.ext].
     */
    public JkStandardFileArtifactProducer<T> setArtifactFilenameComputation(Function<JkArtifactId, Path> artifactFileFunction) {
        JkUtilsAssert.argument(artifactFileFunction != null, "artifactFileFunction cannot be null.");
        this.artifactFileFunction = artifactFileFunction;
        return this;
    }

    public JkStandardFileArtifactProducer<T> putArtifact(JkArtifactId artifactId, Consumer<Path> artifactFileMaker) {
        consumers.put(artifactId, artifactFileMaker);
        return this;
    }

    public JkStandardFileArtifactProducer<T> putMainArtifact(Consumer<Path> artifactFileMaker) {
        return putArtifact(getMainArtifactId(), artifactFileMaker);
    }


    public JkStandardFileArtifactProducer<T> removeArtifact(JkArtifactId artifactId) {
        consumers.remove(artifactId);
        return this;
    }

    @Override
    public String getMainArtifactExt() {
        return mainArtifactExt;
    }

    public JkStandardFileArtifactProducer<T> setMainArtifactExt(String mainArtifactExt) {
        this.mainArtifactExt = mainArtifactExt;
        return this;
    }

}
