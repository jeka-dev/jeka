package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import javax.swing.text.html.Option;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 *  Class behaving as a file supplier for a given dependency.
 */
public final class JkModuleFileProxy {

    private final Supplier<JkRepoSet> repoSetSupplier;

    private final JkModuleDependency moduleDependency;

    private JkModuleFileProxy(Supplier<JkRepoSet> repoSetSupplier, JkModuleDependency moduleDependency) {
        this.repoSetSupplier = repoSetSupplier;
        this.moduleDependency = moduleDependency;
    }

    public static JkModuleFileProxy of(Supplier<JkRepoSet> repoSetSupplier, JkModuleDependency moduleDependency) {
        return new JkModuleFileProxy(repoSetSupplier, moduleDependency);
    }

    public static JkModuleFileProxy of(Supplier<JkRepoSet> repoSetSupplier, String dependencyDescription) {
        return JkModuleFileProxy.of(repoSetSupplier, JkModuleDependency.of(dependencyDescription));
    }

    public static JkModuleFileProxy of(JkRepoSet repoSetSupplier, String dependencyDescription) {
        return of(() -> repoSetSupplier, JkModuleDependency.of(dependencyDescription));
    }

    public Path get() {
        Path result = cachePath(moduleDependency);
        if (!Files.exists(result)) {
            JkLog.trace("File %s not found in cache.", result);
            Path downloadPath = repoSetSupplier.get().get(moduleDependency);
            JkUtilsAssert.state(downloadPath != null, "Dependency %s not resolved", moduleDependency);
            JkUtilsAssert.state(result.equals(downloadPath),
                    "File %s computed for caching %s is different than download file %s. " +
                            "Check the cache path pattern is correct in cachePath() implementation.", result,
                    moduleDependency, downloadPath);
            return result;
        }
        return result;
    }

    private static Path cachePath(JkModuleDependency moduleDependency) {
        String moduleName = moduleDependency.getModuleId().getName();
        Set<JkModuleDependency.JkArtifactSpecification> artifactSpecifications =
                moduleDependency.getArtifactSpecifications();
        JkModuleDependency.JkArtifactSpecification artSpec = !moduleDependency.getArtifactSpecifications().isEmpty() ?
                        moduleDependency.getArtifactSpecifications().iterator().next()
                        : JkModuleDependency.JkArtifactSpecification.of("", "jar");
        String type = JkUtilsString.isBlank(artSpec.getType()) ? "jar" : artSpec.getType();
        String classifierElement = JkUtilsString.isBlank(artSpec.getClassifier()) ? "" : "-" + artSpec.getClassifier();
        String fileName = moduleName + "-" + moduleDependency.getVersion() + classifierElement + "." + type;
        Path path = JkLocator.getJekaRepositoryCache()
                .resolve(moduleDependency.getModuleId().getGroup())
                .resolve(moduleName)
                .resolve(type + "s")
                .resolve(fileName);
        return path;
    }
}
