package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

/**
 * This {@link JkArtifactProducer} produces artifacts files at the path
 * provided by the caller<p/>
 * To add artifacts to produce, caller has to provide the {@link Path} where is generated artifact along
 * the {@link Runnable} for generating it.
 */
public class JkSuppliedFileArtifactProducer<T> implements JkArtifactProducer {

    private final static Supplier<JkPathSequence> EMPTY_SUPPLIER = () -> JkPathSequence.of();

    /**
     * For parent chaining
     */
    public final T __;

    private final Map<JkArtifactId, FileRunnable> fileRunnables = new HashMap<>();

    private JkSuppliedFileArtifactProducer(T __) {
        this.__ = __;
    }

    private String mainArtifactExt = "jar";

    public static <T> JkSuppliedFileArtifactProducer<T> ofParent(T __) {
        return new JkSuppliedFileArtifactProducer<>( __);
    }

    public static JkSuppliedFileArtifactProducer<Void> of() {
        return ofParent(null);
    }

    @Override
    public void makeArtifact(JkArtifactId artifactId) {
        FileRunnable fileRunnable = fileRunnables.get(artifactId);
        if (fileRunnable == null) {
            throw new IllegalArgumentException("No artifact " + artifactId + " defined on this producer. " +
                    "Artifact defined are : " + fileRunnables.entrySet());
        }
        Path path = fileRunnable.file;
        JkLog.startTask("Make artifact " + path.getFileName());
        fileRunnable.runnable.run();
        JkLog.info("Artifact created at " + path);
        JkLog.endTask();
    }

    @Override
    public Path getArtifactPath(JkArtifactId artifactId) {
        FileRunnable fileRunnable = fileRunnables.get(artifactId);
        return fileRunnable == null ? null : fileRunnable.file;
    }

    @Override
    public List<JkArtifactId> getArtifactIds() {
        return new LinkedList<>(fileRunnables.keySet());
    }


    public JkSuppliedFileArtifactProducer<T> putArtifact(JkArtifactId artifactId, Path target, Runnable fileMaker) {
        fileRunnables.put(artifactId, new FileRunnable(target, fileMaker));
        return this;
    }

    public JkSuppliedFileArtifactProducer<T> putMainArtifact(Path target, Runnable fileMaker) {
        return putArtifact(getMainArtifactId(), target, fileMaker);
    }

    public JkSuppliedFileArtifactProducer<T> removeArtifact(JkArtifactId artifactId) {
        fileRunnables.remove(artifactId);
        return this;
    }

    @Override
    public String getMainArtifactExt() {
        return mainArtifactExt;
    }

    public JkSuppliedFileArtifactProducer<T> setMainArtifactExt(String mainArtifactExt) {
        this.mainArtifactExt = mainArtifactExt;
        return this;
    }

    private static class FileRunnable {
        final Runnable runnable;
        final Path file;

        public FileRunnable(Path file, Runnable runnable) {
            this.runnable = runnable;
            this.file = file;
        }
    }


}
