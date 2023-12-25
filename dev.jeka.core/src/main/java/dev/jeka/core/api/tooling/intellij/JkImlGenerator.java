package dev.jeka.core.api.tooling.intellij;

import dev.jeka.core.api.depmanagement.JkQualifiedDependencySet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkRuntime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Provides method to generate and read Eclipse metadata files.
 */
public final class JkImlGenerator {

    private JkPathSequence defClasspath = JkPathSequence.of();

    private JkPathSequence defImportedProjects = JkPathSequence.of();

    /* When true, path will be mentioned with $JEKA_HOME$ and $JEKA_REPO$ instead of explicit absolute path. */
    private boolean useVarPath = true;

    private Path explicitJekaHome;

    private Supplier<JkIdeSupport> ideSupportSupplier;

    // Only used if ideSupport is <code>null</code>
    private Path baseDir;

    private boolean excludeJekaLib;

    private Consumer<JkIml> imlConfigurer = jkIml -> {};

    private boolean failOnDepsResolutionError = true;

    // If true, assumes that jeka dir is the root of the module.
    private boolean dedicatedJekaModule;

    private JkImlGenerator() {
    }

    public static JkImlGenerator of() {
        return new JkImlGenerator();
    }

    /**
     * Returns the .iml file path for a module loacted at the specified root dir.
     * @param moduleRootDir The path of module root dir
     * @param dedicatedJekaModule If true, the .iml file will be located inside the 'jeka' subfilder.
     *                     This is meant for handling case where 'jeka' has its own intellij module
     */
    public static Path getImlFilePath(Path moduleRootDir, boolean dedicatedJekaModule) {
        String fileName = moduleRootDir.getFileName().toString().equals("")
                ? moduleRootDir.toAbsolutePath().getFileName().toString()
                : moduleRootDir.getFileName().toString();
        if (dedicatedJekaModule) {
            fileName = fileName + "-jeka";
            moduleRootDir = moduleRootDir.resolve("jeka");

            // if .iml file is generated in .idea folder, it is not convenient to macos user
            // to import the jeka module as the .iml file will be in a hidde folder
            return JkImlGenerator.findExistingImlFile(moduleRootDir)
                    .orElse(moduleRootDir.resolve(fileName + ".iml"));
        }
        return JkImlGenerator.findExistingImlFile(moduleRootDir)
                .orElse(moduleRootDir.resolve(".idea").resolve(fileName + ".iml"));
    }

    public boolean isUseVarPath() {
        return useVarPath;
    }

    public JkImlGenerator setUseVarPath(boolean useVarPath) {
        this.useVarPath = useVarPath;
        return this;
    }

    public JkImlGenerator setDefClasspath(JkPathSequence defClasspath) {
        this.defClasspath = defClasspath;
        return this;
    }

    public JkImlGenerator setDefImportedProjects(JkPathSequence defImportedProjects) {
        JkUtilsAssert.argument(defImportedProjects != null, "defImportedProjects cannot be null.");
        this.defImportedProjects = defImportedProjects;
        return this;
    }

    public JkImlGenerator setBaseDir(Path baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    public JkImlGenerator setDedicatedJekaModule(boolean dedicatedJekaModule) {
        this.dedicatedJekaModule = dedicatedJekaModule;
        return this;
    }

    public boolean isDedicatedJekaModule() {
        return dedicatedJekaModule;
    }

    public JkImlGenerator configureIml(Consumer<JkIml> imlConfigurer) {
        this.imlConfigurer = this.imlConfigurer.andThen(imlConfigurer);
        return this;
    }

    public Path getExplicitJekaHome() {
        return explicitJekaHome;
    }

    public JkImlGenerator setExplicitJekaHome(Path explicitJekaHome) {
        this.explicitJekaHome = explicitJekaHome;
        return this;
    }

    public JkIdeSupport getIdeSupport() {
        return ideSupportSupplier.get();
    }

    public JkImlGenerator setIdeSupport(Supplier<JkIdeSupport> ideSupportSupplier) {
        this.ideSupportSupplier = ideSupportSupplier;
        return this;
    }

    public JkImlGenerator setIdeSupport(JkIdeSupport ideSupport) {
        this.ideSupportSupplier = () -> ideSupport;
        return this;
    }

    public JkImlGenerator setExcludeJekaLib(boolean excludeJekaLib) {
        this.excludeJekaLib = excludeJekaLib;
        return this;
    }

    public JkImlGenerator setFailOnDepsResolutionError(boolean failOnDepsResolutionError) {
        this.failOnDepsResolutionError = failOnDepsResolutionError;
        return this;
    }

    public JkIml computeIml() {
        JkIdeSupport ideSupport = ideSupportSupplier == null ? null : ideSupportSupplier.get();
        Path dir = ideSupport == null ? baseDir : ideSupport.getProdLayout().getBaseDir();
        JkIml iml = JkIml.of().setModuleDir(dir);
        JkLog.trace("Compute iml def classpath : " + defClasspath);
        JkLog.trace("Compute iml def imported projects : " + defImportedProjects);
        if (this.useVarPath) {
            iml.pathUrlResolver.setPathSubstitute(JkLocator.getCacheDir());
        }
        iml.component
                .getContent()
                    .addJekaStandards(dedicatedJekaModule);
        if (ideSupport != null) {
            List<Path> sourcePaths = ideSupport.getProdLayout().resolveSources().getRootDirsOrZipFiles();
            sourcePaths
                    .forEach(path -> iml.component.getContent().addSourceFolder(path, false, null));
            ideSupport.getProdLayout().resolveResources().getRootDirsOrZipFiles().stream()
                    .filter(path -> !sourcePaths.contains(path))
                    .forEach(path -> iml.component.getContent().addSourceFolder(path, false, "java-resource"));
            List<Path> testSourcePaths = ideSupport.getTestLayout().resolveSources().getRootDirsOrZipFiles();
            testSourcePaths
                    .forEach(path -> iml.component.getContent().addSourceFolder(path, true, null));
            ideSupport.getTestLayout().resolveResources().getRootDirsOrZipFiles().stream()
                    .filter(path -> !testSourcePaths.contains(path))
                    .forEach(path -> iml.component.getContent().addSourceFolder(path, false, "java-test-resource"));
            JkDependencyResolver depResolver = ideSupport.getDependencyResolver();
            JkResolvedDependencyNode tree = depResolver.resolve(
                    ideSupport.getDependencies(),
                    depResolver.getDefaultParams().copy().setFailOnDependencyResolutionError(failOnDepsResolutionError))
                    .getDependencyTree();
            JkLog.trace("Dependencies resolved");
            iml.component.getOrderEntries().addAll(projectOrderEntries(tree));  // too slow
            for (Path path : ideSupport.getGeneratedSourceDirs()) {
                iml.component.getContent().addSourceFolder(path, false, null);
            }
        }
        iml.component.getOrderEntries().addAll(defOrderEntries());
        imlConfigurer.accept(iml);
        JkLog.trace("Iml object generated");
        return iml;
    }

    private Path baseDir() {
        return ideSupportSupplier == null ? baseDir : ideSupportSupplier.get().getProdLayout().getBaseDir();
    }

    // For def, we have computed classpath from runtime
    private List<JkIml.OrderEntry> defOrderEntries() {
        OrderEntries orderEntries = new OrderEntries();
        JkPathSequence importedClasspath = defImportedProjects.getEntries().stream()
                        .map(JkRuntime::get)
                        .map(JkRuntime::getClasspath)
                        .reduce(JkPathSequence.of(), (ps1, ps2) -> ps1.and(ps2))
                        .withoutDuplicates();

        defClasspath.and(defImportedProjects).getEntries().stream()
                .filter(path -> !importedClasspath.getEntries().contains(path))
                .filter(path -> !excludeJekaLib || !JkLocator.getJekaJarPath().equals(path))
                .filter(path -> !path.equals(Paths.get(JkConstants.DEF_BIN_DIR)))
                .forEach(path -> orderEntries.add(path, JkIml.Scope.TEST));
        return new LinkedList<>(orderEntries.orderEntries);
    }

    private class OrderEntries {

        private final LinkedHashSet<JkIml.OrderEntry> orderEntries = new LinkedHashSet<>();

        private final Map<Path, JkIml.ModuleLibraryOrderEntry> cache = new HashMap<>();

        void add(Path entry, JkIml.Scope scope) {
            JkIml.OrderEntry orderEntry;
            if (Files.isDirectory(entry)) {
                orderEntry = dirAsOrderEntry(entry, scope);
            } else {
                orderEntry = cache.computeIfAbsent(entry, JkImlGenerator::libAsOrderEntry)
                        .copy()
                        .setScope(scope)
                        .setExported(true);
            }
            orderEntries.add(orderEntry);
        }
    }

    private Path jekaHome() {
        if (explicitJekaHome != null) {
            return explicitJekaHome;
        }
        return JkLocator.getJekaHomeDir();
    }

    private JkIml.OrderEntry dirAsOrderEntry(Path dir, JkIml.Scope scope) {

        // first, look if it is inside an imported project
        for (Path importedProject : this.defImportedProjects) {
            if (importedProject.resolve(JkConstants.DEF_BIN_DIR).equals(dir)) {
                return JkIml.ModuleOrderEntry.of()
                        .setModuleName(moduleName(dir))
                        .setExported(true)
                        .setScope(scope);
            }
        }
        if (Files.exists(dir.resolve("jeka")) && Files.isDirectory(dir.resolve("jeka"))) {
            return JkIml.ModuleOrderEntry.of().setModuleName(moduleName(dir)).setScope(scope).setExported(true);
        }
        return JkIml.ModuleLibraryOrderEntry.of().setClasses(dir).setScope(scope).setExported(true);
    }

    private static String moduleName(Path projectDir) {
        return findExistingImlFile(projectDir)
                .map(path -> JkUtilsString.substringBeforeLast(path.getFileName().toString(), ".iml"))
                .orElse(projectDir.getFileName().toString());
    }

    private static Optional<Path> findExistingImlFile(Path projectDir) {
        return JkPathTree.of(projectDir).andMatching(".idea/*.iml", "*.iml").stream().findFirst();
    }

    private static JkIml.ModuleLibraryOrderEntry libAsOrderEntry(Path libPath) {
        return JkIml.ModuleLibraryOrderEntry.of()
                .setClasses(libPath)
                .setSources(lookForSources(libPath))
                .setJavadoc(lookForJavadoc(libPath));
    }

    private static Path lookForSources(Path binary) {
        final String name = binary.getFileName().toString();
        final String nameWithoutExt = JkUtilsString.substringBeforeLast(name, ".");
        final String ext = JkUtilsString.substringAfterLast(name, ".");
        final String sourceName = nameWithoutExt + "-sources." + ext;
        final List<Path> folders = JkUtilsIterable.listOf(
                binary.resolve("..").normalize(),
                binary.resolve("../sources").normalize(),
                binary.resolve("../../sources").normalize());
        final List<String> names = JkUtilsIterable.listOf(sourceName, nameWithoutExt + "-sources.zip");
        return lookFileHere(folders, names);
    }

    private static Path lookForJavadoc(Path binary) {
        final String name = binary.getFileName().toString();
        final String nameWithoutExt = JkUtilsString.substringBeforeLast(name, ".");
        final String ext = JkUtilsString.substringAfterLast(name, ".");
        final String sourceName = nameWithoutExt + "-javadoc." + ext;
        final List<Path> folders = JkUtilsIterable.listOf(
                binary.resolve("..").normalize(),
                binary.resolve("../../../libs-javadoc").normalize(),
                binary.resolve("../../libs-javadoc").normalize(),
                binary.resolve("../libs-javadoc").normalize());
        final List<String> names = JkUtilsIterable.listOf(sourceName, nameWithoutExt + "-javadoc.zip");
        return lookFileHere(folders, names);
    }

    private static Path lookFileHere(Iterable<Path> folders, Iterable<String> names) {
        for (final Path folder : folders) {
            for (final String name : names) {
                final Path candidate = folder.resolve(name).normalize();
                if (Files.exists(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private List<JkIml.OrderEntry> projectOrderEntries(JkResolvedDependencyNode tree) {
        long t0 = System.currentTimeMillis();
        OrderEntries orderEntries = new OrderEntries();
        for (final JkResolvedDependencyNode node : tree.toFlattenList()) {

            // Maven dependency
            if (node.isModuleNode()) {
                JkResolvedDependencyNode.JkModuleNodeInfo moduleNodeInfo = node.getModuleInfo();
                if (moduleNodeInfo.isEvicted()) {
                    continue;
                }
                JkIml.Scope scope = ideScope(moduleNodeInfo.getRootConfigurations());
                moduleNodeInfo.getFiles().forEach(path -> orderEntries.add(path, scope));

                // File dependencies (file system + computed)
            } else {
                final JkResolvedDependencyNode.JkFileNodeInfo fileNodeInfo = (JkResolvedDependencyNode.JkFileNodeInfo) node.getNodeInfo();
                JkIml.Scope scope = ideScope(fileNodeInfo.getDeclaredConfigurations());
                if (fileNodeInfo.isComputed()) {
                    final Path projectDir = fileNodeInfo.computationOrigin().getIdeProjectDir();
                    if (projectDir != null) {
                        orderEntries.add(projectDir, scope);
                    }
                } else {
                    fileNodeInfo.getFiles().forEach(path -> orderEntries.add(path, scope));
                }
            }
        }
        JkLog.trace("projectOrderEntries() took '%s ms", System.currentTimeMillis() - t0);
        return new LinkedList(orderEntries.orderEntries);
    }

    private static JkIml.Scope ideScope(Set<String> scopes) {
        if (scopes.contains(JkQualifiedDependencySet.COMPILE_SCOPE))  {
            return JkIml.Scope.COMPILE;
        } else if (scopes.contains(JkQualifiedDependencySet.RUNTIME_SCOPE)) {
            return JkIml.Scope.RUNTIME;
        } else if (scopes.contains(JkQualifiedDependencySet.PROVIDED_SCOPE)) {
            return JkIml.Scope.PROVIDED;
        } else if (scopes.contains(JkQualifiedDependencySet.TEST_SCOPE)) {
            return JkIml.Scope.TEST;
        } else {
            return JkIml.Scope.COMPILE;
        }
    }

}
