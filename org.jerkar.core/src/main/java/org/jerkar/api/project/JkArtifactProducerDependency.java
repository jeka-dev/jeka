package org.jerkar.api.project;

import org.jerkar.api.depmanagement.JkComputedDependency;
import org.jerkar.api.file.JkPath;

import java.io.File;
import java.io.Serializable;


/**
 * Created by angibaudj on 02-08-17.
 */
@Deprecated // Experimental !!!!
public class JkArtifactProducerDependency extends JkComputedDependency  {

    private final JkArtifactProducer artifactProducer;

    public JkArtifactProducerDependency(JkArtifactProducer artifactProducer,
                                           JkArtifactFileId artifactFileId, File baseDir) {
        super(new Invoker(artifactProducer, artifactFileId),
                baseDir, jarAndRuntimeDeps(artifactProducer, artifactFileId).entries());
        this.artifactProducer = artifactProducer;
    }

    public JkArtifactProducerDependency(JkArtifactProducer artifactProducer,
                                        JkArtifactFileId artifactFileId) {
        this(artifactProducer, artifactFileId, null);
    }

    @Override
    public String toString() {
        return artifactProducer.toString();
    }

    private static class Invoker implements Runnable, Serializable {

        private static final long serialVersionUID = 1L;

        private final JkArtifactProducer artifactProducer;

        private final JkArtifactFileId artifactId;

        Invoker(JkArtifactProducer artifactProducer, JkArtifactFileId artifactId) {
            this.artifactProducer = artifactProducer;
            this.artifactId = artifactId;
        }

        @Override
        public void run() {
            artifactProducer.doArtifactFile(artifactId);
        }

        @Override
        public String toString() {
            return this.artifactProducer.toString();
        }
    }

    private static JkPath jarAndRuntimeDeps(JkArtifactProducer producer, JkArtifactFileId artifactId) {
        return producer.runtimeDependencies(artifactId).andHead(producer.artifactFile(artifactId));
    }

}
