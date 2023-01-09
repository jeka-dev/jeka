package dev.jeka.core.api.depmanagement.artifact;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This {@link JkArtifactProducer} produces artifacts files at the pathprovided by the caller<p/>
 * To add artifacts to produce, caller has to provide the {@link Path} where is generated artifact along
 * the {@link Runnable} for generating it.
 */
public class JkSuppliedFileArtifactProducer  implements JkArtifactProducer {

    private final static Supplier<JkPathSequence> EMPTY_SUPPLIER = () -> JkPathSequence.of();

    private final Map<JkArtifactId, FileRunnable> fileRunnables = new HashMap<>();

    private JkSuppliedFileArtifactProducer() {
    }

    private String mainArtifactExt = "jar";

    public static JkSuppliedFileArtifactProducer of() {
        return new JkSuppliedFileArtifactProducer();
    }

    @Override
    public void makeArtifact(JkArtifactId artifactId) {
        FileRunnable fileRunnable = fileRunnables.get(artifactId);
        if (fileRunnable == null) {
            throw new IllegalArgumentException("No artifact " + artifactId + " defined on this producer. " +
                    "Artifact defined are : " + fileRunnables.entrySet());
        }
        Path path = fileRunnable.file;
        JkLog.startTask("Making artifact " + JkUtilsPath.relativizeFromWorkingDir(path));
        fileRunnable.runnable.run();
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


    public JkSuppliedFileArtifactProducer  putArtifact(JkArtifactId artifactId, Path target, Runnable fileMaker) {
        fileRunnables.put(artifactId, new FileRunnable(target, fileMaker));
        return this;
    }

    public JkSuppliedFileArtifactProducer  putMainArtifact(Path target, Runnable fileMaker) {
        return putArtifact(getMainArtifactId(), target, fileMaker);
    }

    public JkSuppliedFileArtifactProducer  removeArtifact(JkArtifactId artifactId) {
        fileRunnables.remove(artifactId);
        return this;
    }

    @Override
    public String getMainArtifactExt() {
        return mainArtifactExt;
    }

    public JkSuppliedFileArtifactProducer  setMainArtifactExt(String mainArtifactExt) {
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
