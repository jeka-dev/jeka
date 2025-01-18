/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.tooling.git;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.project.JkBuildable;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.builtins.base.BaseKBean;

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
        boolean dirty = git.isWorkspaceDirty();
        if (dirty) {
            JkLog.verbose("Git workspace is dirty. Use SNAPSHOT for version.");
            cachedVersion = git.getCurrentBranch() + JkVersion.SNAPSHOT_SUFIX;
        } else {
            cachedVersion =  git.getVersionFromTag(versionTagPrefix);
        }
        JkLog.verbose("Version inferred from Git : %s", cachedVersion);
        return cachedVersion;
    }

    public JkVersion getVersionAsJkVersion() {
        return JkVersion.of(getVersion());
    }

    /**
     * Configures the specified baseKBean to use git version for publishing and adds git info to the manifest.
     */
    public void handleVersioning(JkBuildable.Supplier buildableSupplier) {
        JkBuildable buildable = buildableSupplier.asBuildable();
        if (buildable.getVersion().isUnspecified()) {
            buildable.setVersionSupplier(this::getVersionAsJkVersion);
        }
        JkGit git = JkGit.of(buildable.getBaseDir());
        String commit = git.isWorkspaceDirty() ?  "dirty-" + git.getCurrentCommit() : git.getCurrentCommit();
        buildable.getManifestCustomizers().add(manifest -> manifest
                .addMainAttribute("Git-commit", commit)
                .addMainAttribute("Git-branch", git.getCurrentBranch()));
    }

    /**
     * Convenient static method for handling versioning of both project and base.
     */
    public static void handleVersioning(JkBuildable.Supplier buildableSupplier, String prefix) {
        JkVersionFromGit.of(buildableSupplier.asBuildable().getBaseDir(), prefix)
                .handleVersioning(buildableSupplier);;

    }



}
