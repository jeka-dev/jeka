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

    enum Scope {
        ALL, EXPORTED;

        String prefix() {
            return this == ALL ?  "" : "exported-";
        }

    }

    EngineClasspathCache(Path baseDir, JkDependencyResolver dependencyResolver) {
        this.baseDir = baseDir;
        this.dependencyResolver = dependencyResolver;
    }

    Result resolvedClasspath(JkDependencySet allDependencies, JkDependencySet exportedDependencies, boolean same) {
        PartialResult all = resolvedClasspath(Scope.ALL, allDependencies);
        PartialResult exported = all;
        if (!same) {
            exported = resolvedClasspath(Scope.EXPORTED, exportedDependencies);
        }
        return new Result(all, exported);
    }

    private PartialResult resolvedClasspath(Scope scope, JkDependencySet dependencies) {
        boolean changed = compareAndStore(scope, dependencies);
        if (changed) {
            JkResolveResult resolveResult = dependencyResolver.resolve(dependencies);
            String treeASString = resolveResult.getDependencyTree().toStringTree();
            if (treeASString.trim().isEmpty()) {  // Nicer output
                JkLog.info("Dependency tree of jeka-src : empty.");
            } else {
                JkLog.info("Dependency tree of jeka-src :");
                JkLog.info(resolveResult.getDependencyTree().toStringTree());
            }
            JkPathSequence pathSequence = resolveResult.getFiles();
            storeResolvedClasspath(scope, pathSequence);
            return new PartialResult(true, pathSequence);
        } else {
            if (Files.exists(resolvedClasspathCache(scope))) {
                JkPathSequence cachedPathSequence = readCachedResolvedClasspath(scope);
                JkLog.trace("Cached resolved-classpath : \n" + cachedPathSequence.toPathMultiLine("  "));
                if (cachedPathSequence.hasNonExisting()) {
                    JkLog.trace("Cached classpath contains some non-existing element -> need resolve.");
                    dependencyResolver.resolve(dependencies);
                }
                return new PartialResult(false, cachedPathSequence);
            }
            JkPathSequence resolved = dependencyResolver.resolve(dependencies).getFiles();
            storeResolvedClasspath(scope, resolved);
            return new PartialResult(false, resolved);
        }
    }

    /**
     * Returns true if cached unresolved-classpath is not equals to current one.
     */
    private boolean compareAndStore(Scope scope, JkDependencySet dependencySet) {
        Path cacheFile = unresolvedClasspathCache(scope);
        String content = dependencySet.getEntries().toString();
        if (Files.exists(cacheFile)) {
            String cachedContent = new String(JkUtilsPath.readAllBytes(cacheFile));
            if (content.equals(cachedContent)) {
                JkLog.trace("unresolved-classpath file is still valid -> Jeka classpath will be determined from this.");
                return false;
            }
        }
        JkLog.trace("Update cached 'unresolved-classpath'.");
        if (Files.exists(cacheFile.getParent())) {
            JkPathFile.of(cacheFile).createIfNotExist().write(content.getBytes(StandardCharsets.UTF_8));
        }
        return true;
    }

    private void storeResolvedClasspath(Scope scope, JkPathSequence pathSequence) {
        if (!Files.exists(resolvedClasspathCache(scope).getParent())) {
            return;  // if project dir has no 'jeka' folder, don't store this file.
        }
        JkPathFile.of(resolvedClasspathCache(scope)).createIfNotExist()
                .write(pathSequence.toPath().getBytes(StandardCharsets.UTF_8));
    }

    JkPathSequence readCachedResolvedClasspath(Scope scope) {
        return JkPathSequence.ofPathString(JkPathFile.of(resolvedClasspathCache(scope)).readAsString());
    }

    static class Result {

        final boolean changed;

        final JkPathSequence classpath;

        final JkPathSequence exportedClasspath;

        Result(PartialResult all, PartialResult exported) {
            this.changed = all.changed || exported.changed;
            this.classpath = all.resolvedClasspath;
            this.exportedClasspath = exported.resolvedClasspath;
        }
    }

    private Path unresolvedClasspathCache(Scope scope) {
        return baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve(scope.prefix() + UNRESOLVED_CLASSPATH_FILE);
    }

    private Path resolvedClasspathCache(Scope scope) {
        return baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve(scope.prefix() + RESOLVED_CLASSPATH_FILE);
    }

    private static class PartialResult {

        final boolean changed;

        final JkPathSequence resolvedClasspath;

        public PartialResult(boolean changed, JkPathSequence resolvedClasspath) {
            this.changed = changed;
            this.resolvedClasspath = resolvedClasspath;
        }
    }
}
