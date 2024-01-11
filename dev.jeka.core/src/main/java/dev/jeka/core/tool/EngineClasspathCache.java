package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class EngineClasspathCache {

    private static final String UNRESOLVED_CLASSPATH_FILE = "unresolved-classpath.txt";

    private static final String RESOLVED_CLASSPATH_FILE = "resolved-classpath.txt";

    private final Path baseDir;

    private final JkDependencyResolver dependencyResolver;

    EngineClasspathCache(Path baseDir, JkDependencyResolver dependencyResolver) {
        this.baseDir = baseDir;
        this.dependencyResolver = dependencyResolver;
    }

    Result resolvedClasspath(JkDependencySet allDependencies) {
        boolean changed = compareAndStore(allDependencies);
        if (changed) {
            JkResolveResult resolveResult = dependencyResolver.resolve(allDependencies);
            JkLog.info("Dependency tree of jeka/def :");
            JkLog.info(resolveResult.getDependencyTree().toStringTree());
            JkPathSequence pathSequence = resolveResult.getFiles();
            storeResolvedClasspath(pathSequence);
            return new Result(true, pathSequence);
        } else {
            if (Files.exists(resolvedClasspathCache())) {
                JkPathSequence cachedPathSequence = readCachedResolvedClasspath();
                JkLog.trace("Cached resolved-classpath : \n" + cachedPathSequence.toPathMultiLine("  "));
                if (cachedPathSequence.hasNonExisting()) {
                    JkLog.trace("Cached classpath contains some non-existing element -> need resolve.");
                    dependencyResolver.resolve(allDependencies);
                }
                return new Result(false, cachedPathSequence);
            }
            JkPathSequence resolved = dependencyResolver.resolve(allDependencies).getFiles();
            storeResolvedClasspath(resolved);
            return new Result(false, resolved);
        }
    }

    /**
     * Returns true if cached unresolved-classpath is not equals to current one.
     */
    private boolean compareAndStore(JkDependencySet dependencySet) {
        Path cacheFile = unresolvedClasspathCache();
        String content = dependencySet.getEntries().toString();
        if (Files.exists(cacheFile)) {
            String cachedContent = new String(JkUtilsPath.readAllBytes(cacheFile));
            if (content.equals(cachedContent)) {
                JkLog.trace("unresolved-classpath file is still valid -> Jeka classpath will be determined from this.");
                return false;
            }
        }
        JkLog.trace("Update cached 'unresolved-classpath'.");
        if (Files.exists(cacheFile.getParent().getParent())) {
            JkPathFile.of(cacheFile).createIfNotExist().write(content.getBytes(StandardCharsets.UTF_8));
        }
        return true;
    }

    private void storeResolvedClasspath(JkPathSequence pathSequence) {
        if (!Files.exists(resolvedClasspathCache().getParent().getParent())) {
            return;  // if project dir has no 'jeka' folder, don't store this file.
        }
        JkPathFile.of(resolvedClasspathCache()).createIfNotExist()
                .write(pathSequence.toPath().getBytes(StandardCharsets.UTF_8));
    }

    JkPathSequence readCachedResolvedClasspath() {
        return JkPathSequence.ofPathString(JkPathFile.of(resolvedClasspathCache()).readAsString());
    }

    private Path unresolvedClasspathCache() {
        return baseDir.resolve(JkConstants.WORK_PATH).resolve(UNRESOLVED_CLASSPATH_FILE);
    }

    private Path resolvedClasspathCache() {
        return baseDir.resolve(JkConstants.WORK_PATH).resolve(RESOLVED_CLASSPATH_FILE);
    }

    static class Result {

        final boolean changed;

        final JkPathSequence resolvedClasspath;

        public Result(boolean changed, JkPathSequence resolvedClasspath) {
            this.changed = changed;
            this.resolvedClasspath = resolvedClasspath;
        }
    }
}
