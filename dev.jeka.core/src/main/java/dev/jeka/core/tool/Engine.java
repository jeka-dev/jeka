package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleDependency;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsTime;

import javax.tools.ToolProvider;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Supplier;

import static dev.jeka.core.api.depmanagement.JkDependencySet.Hint.lastAndIf;

/**
 * Engine having responsibility of compiling def classes, instantiate Jeka class, plugins and run it.<br/>
 * Jeka class sources are expected to lie in [project base dir]/jeka/def <br/>
 * Classes having simple name starting with '_' are ignored.
 *
 * Jeka classes can have dependencies on jars : <ul>
 *     <li>located in [base dir]/jeka/boot directory</li>
 *     <li>declared in {@link JkDefClasspath} annotation</li>
 *     <li>declared in command-line, using '@'</li>
 * </ul>
 */
final class Engine {

    private final JkPathMatcher JAVA_DEF_SOURCE_MATCHER = JkPathMatcher.of(true,"**.java")
            .and(false, "**/_*", "_*");

    private final JkPathMatcher KOTLIN_DEF_SOURCE_MATCHER = JkPathMatcher.of(true,"**.kt")
           .and(false, "**/_*", "_*");

    private final Path projectBaseDir;

    private JkDependencySet defDependencies;

    private JkRepoSet defRepos;

    private LinkedHashSet<Path> rootsOfImportedJekaClasses = new LinkedHashSet<>();

    private List<String> compileOptions = new LinkedList<>();

    private final ClassResolver resolver;

    /**
     * Constructs an engine for the specified base directory.
     */
    Engine(Path baseDir) {
        super();
        this.projectBaseDir = baseDir.normalize();
        defRepos = repos();
        this.defDependencies = JkDependencySet.of();
        this.resolver = new ClassResolver(baseDir);
    }

    <T extends JkClass> T getJkClass(Class<T> baseClass, boolean initialise) {
        if (resolver.needCompile()) {
            this.resolveAndCompile(true);
        }
        return resolver.resolve(baseClass, initialise);
    }

    /**
     * Pre-compile and compile Jeka classes (if needed) then execute methods mentioned in command line
     */
    void execute(CommandLine commandLine, JkLog.Verbosity verbosityToRestore) {
        final long start = System.nanoTime();
        JkLog.startTask("Compile def and initialise Jeka classes");
        List<JkModuleDependency> commandLineDependencies = commandLine.getDefDependencies();
        JkLog.trace("Add following dependencies to Jeka classpath : " + commandLineDependencies);
        defDependencies = defDependencies
                .and(commandLineDependencies)
                .and(dependenciesOnJeka());
        JkClass jkClass = null;
        JkPathSequence path;
        String jkClassHint = Environment.standardOptions.jkClassName();
        preCompile();  // Need to pre-compile to get the declared def dependencies

        // First try to instantiate class without compiling and resolving if a jeka class
        // has been specified and no extra dependencies defined in command line.
        if (!JkUtilsString.isBlank(jkClassHint) && Environment.commandLine.getDefDependencies().isEmpty()) {  // First find a class in the existing classpath without compiling
            jkClass = getJkClassInstance(jkClassHint, JkPathSequence.of());
        }

        // No Jkclass has been foud
        if (jkClass == null) {
            path = resolveAndCompile(true);
            jkClass = getJkClassInstance(jkClassHint, path);
            if (jkClass == null) {
                String hint = JkUtilsString.isBlank(jkClassHint) ? "" : " named " + jkClassHint;
                String prompt = !JkUtilsString.isBlank(jkClassHint) ? ""
                        : "\nAre you sure this directory is a Jeka project ?";
                throw new JkException("Can't find or guess any Jeka class%s in project %s.%s",
                        hint, this.projectBaseDir, prompt);
            }
        } else {
            path = resolveAndCompile(false);
        }
        jkClass.getImportedJkClasses().setImportedRunRoots(this.rootsOfImportedJekaClasses);
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        JkLog.info("Jeka classes are ready to be executed.");
        JkLog.setVerbosity(verbosityToRestore);
        if (Environment.standardOptions.logRuntimeInformation != null) {
            JkLog.info("Jeka Classpath : ");
            path.iterator().forEachRemaining(item -> JkLog.info("    " + item));
        }
        try {
            this.launch(jkClass, commandLine);
        } catch (final RuntimeException e) {
            JkLog.error("Engine " + projectBaseDir + " failed");
            throw e;
        }
    }

    private void preCompile() {
        final List<Path> sourceFiles = JkPathTree.of(resolver.defSourceDir)
                .andMatcher(JAVA_DEF_SOURCE_MATCHER.or(KOTLIN_DEF_SOURCE_MATCHER)).getFiles();
        JkLog.trace("Parse source code of " + sourceFiles);
        final SourceParser parser = SourceParser.of(this.projectBaseDir, sourceFiles);
        this.defDependencies = this.defDependencies.and(parser.dependencies());
        this.defRepos = parser.importRepos().and(defRepos);
        this.rootsOfImportedJekaClasses = parser.projects();
        this.compileOptions = parser.compileOptions();
    }

    /*
     * Resolves dependencies and compiles and sources classes contained in jeka/def.
     * It returns a path sequence containing the resolved dependencies and result of compilation.
     */
    private JkPathSequence resolveAndCompile(boolean compileSources) {
        return resolveAndCompile(new HashSet<>(), JkPathSequence.of(), compileSources);
    }

    private JkPathSequence resolveAndCompile(Set<Path> yetCompiledProjects, JkPathSequence path, boolean compileSources) {
        if (!this.resolver.hasDefSource() || yetCompiledProjects.contains(this.projectBaseDir)) {
            if (Environment.commandLine.getDefDependencies().isEmpty()) {
                return JkPathSequence.of();
            } else {
                return dependenciesPath();
            }
        }
        yetCompiledProjects.add(this.projectBaseDir);
        preCompile(); // This enrich dependencies
        final String msg = "Compiling def classes for project " + this.projectBaseDir.getFileName().toString();
        final long start = System.nanoTime();
        JkLog.startTask(msg);
        JkPathSequence dependencyPath = dependenciesPath().andPrepend(path).withoutDuplicates();
        JkPathSequence projectDependenciesPath =
                resolveAndCompileDependentProjects(yetCompiledProjects, dependencyPath, compileSources);
        if (compileSources) {
            compileDef(dependencyPath.and(projectDependenciesPath));
        }
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        return dependencyPath.and(projectDependenciesPath).and(this.resolver.defClassDir).withoutDuplicates();
    }

    private JkPathSequence dependenciesPath() {
        final JkDependencyResolver defDependencyResolver = getDefDependencyResolver();
        final JkResolveResult resolveResult = defDependencyResolver.resolve(this.defDependencies);
        if (resolveResult.getErrorReport().hasErrors()) {
            JkLog.warn(resolveResult.getErrorReport().toString());
        }
        return resolveResult.getFiles().withoutDuplicates();
    }

    private JkClass getJkClassInstance(String jkClassHint, JkPathSequence runtimePath) {
        final JkUrlClassLoader classLoader = JkUrlClassLoader.ofCurrent();
        classLoader.addEntries(runtimePath);
        JkLog.trace("Setting def execution classpath to : " + classLoader.getDirectClasspath());
        final JkClass jkClass = resolver.resolve(jkClassHint);
        if (jkClass == null) {
            return null;
        }
        try {
            jkClass.setDefDependencyResolver(this.defDependencies, getDefDependencyResolver());
            return jkClass;
        } catch (final RuntimeException e) {
            JkLog.error("Engine " + projectBaseDir + " failed");
            throw e;
        }
    }

    private JkDependencySet dependenciesOnJeka() {

        // If true, we assume Jeka is provided by IDE (development mode)
        final boolean devMode = Files.isDirectory(JkLocator.getJekaJarPath());
        return defDependencies
                .andFiles(bootLibs())
                .andFiles(lastAndIf(!devMode), JkClasspath.ofCurrentRuntime())
                .andFiles(lastAndIf(devMode), JkLocator.getJekaJarPath());
    }

    private JkPathSequence bootLibs() {
        final List<Path>  extraLibs = new LinkedList<>();
        final Path bootDir = this.projectBaseDir.resolve(JkConstants.BOOT_DIR);
        if (Files.exists(bootDir)) {
            extraLibs.addAll(JkPathTree.of(bootDir).andMatching(true,"**.jar").getFiles());
        }
        return JkPathSequence.of(extraLibs).withoutDuplicates();
    }

    /*
     * Resolves dependencies and compiles project that this one depends on.
     * It returns a path resulting of the dependency resolution and compilation output.
     */
    private JkPathSequence resolveAndCompileDependentProjects(Set<Path> yetCompiledProjects,
                                                              JkPathSequence compilePath,
                                                              boolean compileSources) {
        boolean compileImports = !this.rootsOfImportedJekaClasses.isEmpty();
        if (compileImports) {
            JkLog.startTask("Compile Jeka classes of dependent projects : "
                    + toRelativePaths(this.projectBaseDir, this.rootsOfImportedJekaClasses));
        }
        JkPathSequence inputPath = compilePath;
        JkPathSequence result = JkPathSequence.of();
        for (final Path file : this.rootsOfImportedJekaClasses) {
            final Engine engine = new Engine(file.toAbsolutePath().normalize());
            JkPathSequence resultPath = engine.resolveAndCompile(yetCompiledProjects, inputPath, compileSources);
            inputPath = inputPath.and(resultPath);
            result = result.and(resultPath);
        }
        if (compileImports) {
            JkLog.endTask();
        }
        return result.withoutDuplicates();
    }

    private void compileDef(JkPathSequence defClasspath) {
        JkPathTree.of(resolver.defClassDir).deleteContent();
        if (hasKotlin()) {
            final JkKotlinJvmCompileSpec kotlinCompileSpec = defKotlinCompileSpec(defClasspath);
            JkKotlinCompiler kotlinCompiler = JkKotlinCompiler.ofKotlinHome().addOption("-nowarn");
            wrapCompile(() -> kotlinCompiler.compile(kotlinCompileSpec));
            JkUrlClassLoader classLoader = JkUrlClassLoader.ofCurrent();
            classLoader.addEntries(kotlinCompiler.getStdLib());
        }
        final JkJavaCompileSpec javaCompileSpec = defJavaCompileSpec(defClasspath);
        if (!javaCompileSpec.computeJavacSourceArguments().isEmpty() && ToolProvider.getSystemJavaCompiler() == null) {
            throw new JkException("The running Java platform (" +  System.getProperty("java.home") +
                    ") does not provide compiler (javac). Please provide a JDK java platform by pointing JAVA_HOME" +
                    " or JEKA_JDK environment variable to a JDK directory.");
        }
        wrapCompile(() -> JkJavaCompiler.of().compile(javaCompileSpec));
        JkPathTree.of(this.resolver.defSourceDir)
                .andMatching(false, "**/*.java", "*.java", "**/*.kt", "*.kt")
                .copyTo(this.resolver.defClassDir, StandardCopyOption.REPLACE_EXISTING);
    }

    private void wrapCompile(Supplier<Boolean> compileTask) {
        boolean success = compileTask.get();
        if (!success) {
            throw new JkException("Compilation of Jeka files failed. You can run jeka -JKC= to use default JkClass " +
                    " instead of the ones defined in 'def'.");
        }
    }

    private void launch(JkClass jkClass, CommandLine commandLine) {
        if (!commandLine.getSubProjectMethods().isEmpty()) {
            for (final JkClass importedJkClass : jkClass.getImportedJkClasses().getAll()) {
                runProject(importedJkClass, commandLine.getSubProjectMethods());
            }
            runProject(jkClass, commandLine.getSubProjectMethods());
        }
        List<CommandLine.MethodInvocation> methods = commandLine.getMasterMethods();
        if (methods.isEmpty() && Environment.standardOptions.logRuntimeInformation == null) {
            methods = Collections.singletonList(CommandLine.MethodInvocation.normal(JkConstants.DEFAULT_METHOD));
        }
        runProject(jkClass, methods);
    }

    private boolean hasKotlin() {
        return JkPathTree.of(resolver.defSourceDir).andMatcher(KOTLIN_DEF_SOURCE_MATCHER)
                .count(1, false) > 0;
    }

    private JkJavaCompileSpec defJavaCompileSpec(JkPathSequence classpath) {
        final JkPathTree defSource = JkPathTree.of(resolver.defSourceDir).andMatcher(JAVA_DEF_SOURCE_MATCHER);
        JkUtilsPath.createDirectories(resolver.defClassDir);
        return JkJavaCompileSpec.of()
                .setClasspath(classpath.and(resolver.defClassDir))
                .setOutputDir(resolver.defClassDir)
                .addSources(defSource)
                .addOptions(this.compileOptions);
    }

    private JkKotlinJvmCompileSpec defKotlinCompileSpec(JkPathSequence defClasspath) {
        JkUtilsPath.createDirectories(resolver.defClassDir);
        return JkKotlinJvmCompileSpec.of()
                .setClasspath(defClasspath)
                .addSources(resolver.defSourceDir)
                .setOutputDir(resolver.defClassDir);
    }

    private JkDependencyResolver getDefDependencyResolver() {
        return JkDependencyResolver.of().addRepos(this.defRepos);
    }

    private static void runProject(JkClass jkClass, List<CommandLine.MethodInvocation> invokes) {
        for (final CommandLine.MethodInvocation methodInvocation : invokes) {
            invokeMethodOnCommandSetOrPlugin(jkClass, methodInvocation);
        }
    }

    private static void invokeMethodOnCommandSetOrPlugin(JkClass jkClass,
                                                         CommandLine.MethodInvocation methodInvocation) {
        if (methodInvocation.pluginName != null) {
            final JkPlugin plugin = jkClass.getPlugins().get(methodInvocation.pluginName);
            invokeMethodOnCommandsOrPlugin(plugin, methodInvocation.methodName);
        } else {
            invokeMethodOnCommandsOrPlugin(jkClass, methodInvocation.methodName);
        }
    }

    /**
     * Invokes the specified method in this run.
     */
    private static void invokeMethodOnCommandsOrPlugin(Object run, String methodName) {
        final Method method;
        try {
            method = run.getClass().getMethod(methodName);
        } catch (final NoSuchMethodException e) {
            throw new JkException("No public zero-arg method '" + methodName + "' found in class '" + run.getClass());
        }
        String fullMethodName = run.getClass().getName() + "#" + methodName;
        if (Environment.standardOptions.logSetup) {
            JkLog.startTask("\nExecute method : " + fullMethodName);
        }
        try {
            JkUtilsReflect.invoke(run, method);
            if (Environment.standardOptions.logSetup) {
                JkLog.endTask("Method " + fullMethodName + " succeeded in %d milliseconds.");
            }
        } catch (final RuntimeException e) {
            if (Environment.standardOptions.logSetup) {
                JkLog.endTask("Method " + fullMethodName + " failed in %d milliseconds.");
            }
            throw e;
        }
    }

    static JkRepoSet repos() {
        return JkRepoSet.of(JkRepoConfigOptionLoader.defRepository(), JkRepo.ofLocal());
    }

    private static List<String> toRelativePaths(Path from, LinkedHashSet<Path>  files) {
        final List<String> result = new LinkedList<>();
        for (final Path file : files) {
            final String relPath = from.relativize(file).toString();
            result.add(relPath);
        }
        return result;
    }

    @Override
    public String toString() {
        return this.projectBaseDir.getFileName().toString();
    }

}
