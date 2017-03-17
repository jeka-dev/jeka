package org.jerkar.api.depmanagement;

/**
 * Created by djeang on 17-03-17.
 */
public final class JkArtifactDef {

    private final JkVersionedModule versionedModule;

    private final String classifier;

    public static JkArtifactDef of(JkVersionedModule versionedModule, String classifier) {
        return new JkArtifactDef(versionedModule, classifier);
    }

    public static JkArtifactDef of(JkVersionedModule versionedModule) {
        return of(versionedModule, null);
    }

    private JkArtifactDef(JkVersionedModule versionedModule, String classifier) {
        this.versionedModule = versionedModule;
        this.classifier = classifier;
    }

    public JkVersionedModule getVersionedModule() {
        return versionedModule;
    }

    public String getClassifier() {
        return classifier;
    }

    @Override
    public String toString() {
        if (classifier == null) {
            return versionedModule.toString();
        }
        return versionedModule  +
                "-" + classifier;
    }
}
