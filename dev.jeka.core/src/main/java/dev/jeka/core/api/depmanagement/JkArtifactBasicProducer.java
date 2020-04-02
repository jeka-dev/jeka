package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class JkArtifactBasicProducer<T> implements JkArtifactProducer {

    /**
     * For parent chaining
     */
    public final T __;

    private final Map<JkArtifactId, Consumer<Path>> consumers = new HashMap<>();

    private Function<JkArtifactId, JkPathSequence> artifactRunDepsFunction;

    private Function<JkArtifactId, Path> artifactFileFunction;

    private JkArtifactBasicProducer(T __) {
        this.__ = __;
    }

    public static <T> JkArtifactBasicProducer<T> of(T __) {
        return new JkArtifactBasicProducer<>( __);
    }

    public static JkArtifactBasicProducer<Void> of() {
        return of(null);
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
    public JkPathSequence fetchRuntimeDependencies(JkArtifactId artifactId) {
        return artifactRunDepsFunction.apply(artifactId);
    }

    @Override
    public Path getArtifactPath(JkArtifactId artifactId) {
        return artifactFileFunction.apply(artifactId);
    }

    @Override
    public Iterable<JkArtifactId> getArtifactIds() {
        return consumers.keySet();
    }


    public JkArtifactBasicProducer<T> setArtifactRunDepsFunction(Function<JkArtifactId, JkPathSequence> artifactRunDepsFunction) {
        this.artifactRunDepsFunction = artifactRunDepsFunction;
        return this;
    }

    public JkArtifactBasicProducer<T> setArtifactFileFunction(Function<JkArtifactId, Path> artifactFileFunction) {
        this.artifactFileFunction = artifactFileFunction;
        return this;
    }

    public JkArtifactBasicProducer<T> setArtifactFileFunction(Supplier<Path> targetDir, Supplier<String> partName) {
        return setArtifactFileFunction(artifactId -> targetDir.get().resolve(artifactId.toFileName(partName.get())));
    }

    public JkArtifactBasicProducer<T> putArtifact(JkArtifactId artifactId, Consumer<Path> path) {
        consumers.put(artifactId, path);
        return this;
    }

    public JkArtifactBasicProducer<T> putMainArtifact(Consumer<Path> path) {
        consumers.put(getMainArtifactId(), path);
        return this;
    }

    public JkArtifactBasicProducer<T> removeArtifact(JkArtifactId artifactId) {
        consumers.remove(artifactId);
        return this;
    }


}
