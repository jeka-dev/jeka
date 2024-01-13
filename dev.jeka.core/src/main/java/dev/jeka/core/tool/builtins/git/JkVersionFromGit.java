package dev.jeka.core.tool.builtins.git;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.JkGit;
import dev.jeka.core.tool.builtins.self.SelfAppKBean;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Version calculator from git info.
 * The version is computed according current branch and git tags.
 */
public class JkVersionFromGit  {

    private final String versionTagPrefix;

    private final Path baseDir;

    private transient String cachedVersion;

    private JkVersionFromGit(Path baseDir, String versionTagPrefix) {
        this.baseDir = baseDir;
        this.versionTagPrefix = versionTagPrefix;
    }

    public static JkVersionFromGit of(Path baseDir, String versionTagPrefix) {
        return new JkVersionFromGit(baseDir, versionTagPrefix);
    }

    public static JkVersionFromGit of(String versionTagPrefix) {
        return of(Paths.get(""), versionTagPrefix);
    }

    public static JkVersionFromGit of() {
        return of("");
    }

    public String getVersionTagPrefix() {
        return versionTagPrefix;
    }

    public Path getBaseDir() {
        return baseDir;
    }

    /**
     * Gets the current version either from commit message if specified nor from git tag.
     */
    public String getVersion() {
        if (cachedVersion != null) {
            return cachedVersion;
        }
        JkGit git = JkGit.of(baseDir);
        boolean dirty = JkGit.of(this.baseDir).isWorkspaceDirty();
        if (dirty) {
            JkLog.trace("Git workspace is dirty. Use SNAPSHOT for version.");
            cachedVersion = git.getCurrentBranch() + JkVersion.SNAPSHOT_SUFIX;
        } else {
            cachedVersion =  git.getVersionFromTag(versionTagPrefix);
        }
        JkLog.info("Version inferred from Git:" + cachedVersion);
        return cachedVersion;
    }

    public JkVersion getVersionAsJkVersion() {
        return JkVersion.of(getVersion());
    }

    /**
     * Configures the specified project to use git version for publishing and adds git info to the manifest.
     */
    public void handleVersioning(JkProject project) {
        if (project.getVersion().isUnspecified()) {
            project.setVersionSupplier(this::getVersionAsJkVersion);
        }
        JkGit git = JkGit.of(project.getBaseDir());
        String commit = git.isWorkspaceDirty() ?  "dirty-" + git.getCurrentCommit() : git.getCurrentCommit();
        project.packaging.manifestCustomizer.add(manifest -> manifest
                .addMainAttribute("Git-commit", commit)
                .addMainAttribute("Git-branch", git.getCurrentBranch()));
    }

    /**
     * Configures the specified selfAppKBean to use git version for publishing and adds git info to the manifest.
     */
    public void handleVersioning(SelfAppKBean selfAppKBean) {
        if (selfAppKBean.getVersion().isUnspecified()) {
            selfAppKBean.setVersionSupplier(this::getVersionAsJkVersion);
        }
        JkGit git = JkGit.of(selfAppKBean.getBaseDir());
        String commit = git.isWorkspaceDirty() ?  "dirty-" + git.getCurrentCommit() : git.getCurrentCommit();
        selfAppKBean.manifestCustomizers.add(manifest -> manifest
                .addMainAttribute("Git-commit", commit)
                .addMainAttribute("Git-branch", git.getCurrentBranch()));
    }



}
