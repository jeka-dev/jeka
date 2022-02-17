package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 *  Class behaving as a file supplier for a given dependency.
 */
public final class JkModuleFileProxy {

    private final JkRepoSet repoSet;

    private final JkModuleDependency moduleDependency;

    private JkModuleFileProxy(JkRepoSet repoSet, JkModuleDependency moduleDependency) {
        this.repoSet = repoSet;
        this.moduleDependency = moduleDependency;
    }

    public static JkModuleFileProxy of(JkRepoSet repoSet, JkModuleDependency moduleDependency) {
        return new JkModuleFileProxy(repoSet, moduleDependency);
    }

    public static JkModuleFileProxy of(JkRepoSet repoSet, String dependencyDescription) {
        return of(repoSet, JkModuleDependency.of(dependencyDescription));
    }

    public static JkModuleFileProxy ofStandardRepos(String dependencyDescription) {
        return of(JkRepoFromProperties.getDownloadRepos().and(JkRepo.ofMavenCentral()), dependencyDescription);
    }

    public Path get() {
        Path result = moduleDependency.cachePath();
        if (!Files.exists(result)) {
            JkLog.trace("File %s not found in cache.", result);
            Path downloadPath = repoSet.get(moduleDependency);
            JkUtilsAssert.state(downloadPath != null, "Dependency %s not resolved", moduleDependency);
            JkUtilsAssert.state(result.equals(downloadPath),
                    "File %s computed for caching %s is different than download file %s. " +
                            "Check the cache path pattern is correct in cachePath() implementation.", result,
                    moduleDependency, downloadPath);
            return result;
        }
        return result;
    }


}
