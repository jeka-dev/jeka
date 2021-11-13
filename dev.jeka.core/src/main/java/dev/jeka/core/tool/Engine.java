package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.*;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.system.JkBusyIndicator;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkMemoryBufferLogDecorator;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsTime;

import javax.tools.ToolProvider;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Supplier;

import static dev.jeka.core.api.depmanagement.JkDependencySet.Hint.lastAndIf;

/**
 * Engine having responsibility of compile def classes, instantiate KBeans and run.<br/>
 * Sources are expected to lie in [project base dir]/jeka/def <br/>
 * Def classes having simple name starting with '_' are ignored.
 *
 * Def classes can have dependencies on jars : <ul>
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

    private final static String LAST_UPDATE_FILE_NAME = "def-last-update-time.txt";

    private final Path projectBaseDir;

    private JkDependencySet defDependencies;

    private LinkedHashSet<Path> rootsOfImportedJekaClasses = new LinkedHashSet<>();

    private List<String> compileOptions = new LinkedList<>();

    private final ClassResolver resolver;

    /**
     * Constructs an engine for the specified base directory.
     */
    Engine(Path baseDir) {
        super();
        this.projectBaseDir = baseDir.normalize();
        this.defDependencies = JkDependencySet.of();
        this.resolver = new ClassResolver(baseDir);
    }

    // Used by JkImportedJkBeans for resolving transitive import
    <T extends JkBean> T getJkBean(Class<T> baseClass, boolean initialise) {
        if (resolver.needCompile()) {
            this.resolveDependenciesAndCompile(true);
        }
        return (T) resolver.resolveDefaultJkBean();
    }

    /**
     * Pre-compile and compile Jeka classes (if needed) then execute methods mentioned in command line
     */
    void execute(CommandLine commandLine) {
        final long start = System.nanoTime();
        JkLog.startTask("Compile def and initialise KBeans");
        List<JkDependency> commandLineDependencies = commandLine.getDefDependencies();
        JkLog.trace("Add following dependencies to def classpath : " + commandLineDependencies);
        defDependencies = defDependencies
                .and(commandLineDependencies)
                .and(dependenciesOnJeka());
        String jkClassHint = Environment.standardOptions.jkClassName();
        preCompile();  // Need to pre-compile to get the declared def dependencies
        JkPathSequence computedClasspath = resolveDependenciesAndCompile(true);
        JkRuntime.BASE_DIR_CONTEXT.set(projectBaseDir);
        JkRuntime runtime = JkRuntime.get();
        runtime.setDependenciesAndResolver(defDependencies, getDefDependencyResolver());
        runtime.setImportedProjectDirs(this.rootsOfImportedJekaClasses);
        JkBean jkBean = resolver.resolveDefaultJkBean();
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        JkLog.info("Jeka classes are ready to be executed.");

        if (JkMemoryBufferLogDecorator.isActive()) {
            JkBusyIndicator.stop();
            JkMemoryBufferLogDecorator.inactivateOnJkLog();
        }
        if (Environment.standardOptions.logRuntimeInformation != null) {
            JkLog.info("Jeka Classpath : ");
            computedClasspath.iterator().forEachRemaining(item -> JkLog.info("    " + item));
        }
        try {
            this.launch(jkBean, commandLine);
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
        this.rootsOfImportedJekaClasses = parser.projects();
        this.compileOptions = parser.compileOptions();
    }

    /*
     * Resolves dependencies and compiles and sources classes contained in jeka/def.
     * It returns a path sequence containing the resolved dependencies and result of compilation.
     */
    private JkPathSequence resolveDependenciesAndCompile(boolean compileSources) {
        return resolveDependenciesAndCompile(new HashSet<>(), JkPathSequence.of(), compileSources);
    }

    private JkPathSequence resolveDependenciesAndCompile(Set<Path> yetCompiledProjects, JkPathSequence path, boolean compileSources) {
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
        JkPathSequence dependencyPath = dependenciesPath().andPrepend(path).andPrepend(bootLibs()).withoutDuplicates();
        JkPathSequence projectDependenciesPath =
                resolveAndCompileDependentProjects(yetCompiledProjects, dependencyPath, compileSources);
        long defLastUptateTime = lastModifiedAccordingFileAttributes();
        if (compileSources) {
            if (Environment.standardOptions.forceCompile() || isWorkOutdated(defLastUptateTime)) {
                compileDef(dependencyPath.and(projectDependenciesPath));
                writeLastUpdateFile(defLastUptateTime, JkJavaVersion.ofCurrent());
            } else {
                JkLog.trace("Last def classes are up-to-date : No need to compile.");
            }

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
            return jkClass;
        } catch (final RuntimeException e) {
            JkLog.error("Engine " + projectBaseDir + " failed");
            throw e;
        }
    }

    private JkBean findDefaultJkBeanInstance(JkPathSequence runtimePath) {
        final JkUrlClassLoader classLoader = JkUrlClassLoader.ofCurrent();
        classLoader.addEntries(runtimePath);
        JkLog.trace("Setting def execution classpath to : " + classLoader.getDirectClasspath());
        final JkBean jkBean = resolver.resolveDefaultJkBean();
        return jkBean;
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
            JkPathSequence resultPath = engine.resolveDependenciesAndCompile(yetCompiledProjects, inputPath, compileSources);
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
            JkKotlinCompiler kotlinCompiler = JkKotlinCompiler.ofJvm(getDefDependencyResolver().getRepos())
                    .setLogOutput(true)
                    .addOption("-nowarn");
            JkPathSequence kotlinClasspath = defClasspath.and(kotlinCompiler.getStdJdk8Lib());
            final JkKotlinJvmCompileSpec kotlinCompileSpec = defKotlinCompileSpec(kotlinClasspath);

            if (JkLog.isVerbose()) {
                kotlinCompiler.addOption("-verbose");
            }
            wrapCompile(() -> kotlinCompiler.compile(kotlinCompileSpec));
            JkUrlClassLoader classLoader = JkUrlClassLoader.ofCurrent();
            if (kotlinCompiler.isProvidedCompiler()) {
                classLoader.addEntries(kotlinCompiler.getStdLib());
            }
        }
        final JkJavaCompileSpec javaCompileSpec = defJavaCompileSpec(defClasspath);
        if (javaCompileSpec.getSources().containFiles() && ToolProvider.getSystemJavaCompiler() == null) {
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

    private void launch(JkBean jkBean, CommandLine commandLine) {
        List<CommandLine.MethodInvocation> methods = commandLine.getMasterMethods();
        if (methods.isEmpty() && Environment.standardOptions.logRuntimeInformation == null) {
            methods = Collections.singletonList(CommandLine.MethodInvocation.normal(JkConstants.DEFAULT_METHOD));
        }
        runProject(jkBean, methods);
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
                .setSources(defSource.toSet())
                .addOptions(this.compileOptions);
    }

    private JkKotlinJvmCompileSpec defKotlinCompileSpec(JkPathSequence defClasspath) {
        JkUtilsPath.createDirectories(resolver.defClassDir);
        return JkKotlinJvmCompileSpec.of()
                .setClasspath(defClasspath)
                .setSources(JkPathTreeSet.of(resolver.defSourceDir))
                .setOutputDir(JkUtilsPath.relativizeFromWorkingDir(resolver.defClassDir));
    }

    private JkDependencyResolver getDefDependencyResolver() {
        return JkDependencyResolver.of().addRepos(JkRepoFromOptions.getDownloadRepo(), JkRepo.ofLocal());
    }

    private static void runProject(JkBean jkBean, List<CommandLine.MethodInvocation> invokes) {
        for (final CommandLine.MethodInvocation methodInvocation : invokes) {
            invokeMethodOnCommandSetOrPlugin(jkBean, methodInvocation);
        }
    }

    private static void invokeMethodOnCommandSetOrPlugin(JkBean jkBean, CommandLine.MethodInvocation methodInvocation) {
        if (methodInvocation.pluginName != null) {
            final JkBean invokedJkBean = jkBean.getRuntime().getBeanRegistry().get(methodInvocation.pluginName);
            invokeMethodOnJkBean(jkBean, methodInvocation.methodName);
        } else {
            invokeMethodOnJkBean(jkBean, methodInvocation.methodName);
        }
    }

    /**
     * Invokes the specified method on the specified KBean.
     */
    private static void invokeMethodOnJkBean(Object jkBean, String methodName) {
        final Method method;
        try {
            method = jkBean.getClass().getMethod(methodName);
        } catch (final NoSuchMethodException e) {
            throw new JkException("No public zero-arg method '" + methodName + "' found in class '" + jkBean.getClass());
        }
        String fullMethodName = jkBean.getClass().getName() + "#" + methodName;
        if (Environment.standardOptions.logSetup) {
            JkLog.startTask("\nExecute method : " + fullMethodName);
        }
        try {
            JkUtilsReflect.invoke(jkBean, method);
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
        return JkRepoSet.of(JkRepoFromOptions.getDownloadRepo(), JkRepo.ofLocal());
    }

    private static List<String> toRelativePaths(Path from, LinkedHashSet<Path>  files) {
        final List<String> result = new LinkedList<>();
        for (final Path file : files) {
            final String relPath = from.relativize(file).toString();
            result.add(relPath);
        }
        return result;
    }

    private boolean isWorkOutdated(long lastModifiedAccordingFileAttributes) {
        TimestampAndJavaVersion timestampAndJavaVersion = lastModifiedAccordingFlag();
        return timestampAndJavaVersion.timestamp < lastModifiedAccordingFileAttributes
                || !JkJavaVersion.ofCurrent().equals(timestampAndJavaVersion.javaVersion);
    }

    private long lastModifiedAccordingFileAttributes() {
        Path def = projectBaseDir.resolve(JkConstants.DEF_DIR);
        return JkPathTree.of(def).stream()
                .map(path -> JkUtilsPath.getLastModifiedTime(path))
                .map(optional -> optional.orElse(System.currentTimeMillis()))
                .reduce(0L, Math::max);
    }

    private void writeLastUpdateFile(long lastModifiedAccordingFileAttributes, JkJavaVersion javaVersion) {
        Path work = projectBaseDir.resolve(JkConstants.WORK_PATH);
        if (!Files.exists(work)) {
            return;
        }
        String infoString = Long.toString(lastModifiedAccordingFileAttributes) + ";" + javaVersion;
        JkPathFile.of(work.resolve(LAST_UPDATE_FILE_NAME))
                .deleteIfExist()
                .createIfNotExist()
                .write(infoString.getBytes(StandardCharsets.UTF_8));
    }

    private TimestampAndJavaVersion lastModifiedAccordingFlag() {
        Path work = projectBaseDir.resolve(JkConstants.WORK_PATH);
        if (!Files.exists(work)) {
            return new TimestampAndJavaVersion(0L, JkJavaVersion.ofCurrent());
        }
        Path lastUpdateFile = work.resolve(LAST_UPDATE_FILE_NAME);
        if (!Files.exists(lastUpdateFile)) {
            return new TimestampAndJavaVersion(0L, JkJavaVersion.ofCurrent());
        }
        try {
            String content = JkUtilsPath.readAllLines(lastUpdateFile).get(0);
            String[] items = content.split(";");
            return new TimestampAndJavaVersion(Long.parseLong(items[0]), JkJavaVersion.of(items[1]));
        } catch (RuntimeException e) {
            JkLog.warn("Error caught when reading file content of " + lastUpdateFile + ". " + e.getMessage() );
            return new TimestampAndJavaVersion(0L, JkJavaVersion.ofCurrent());
        }
    }

    private static class TimestampAndJavaVersion {

        final long timestamp;

        final JkJavaVersion javaVersion;

        public TimestampAndJavaVersion(long timestamp, JkJavaVersion javaVersion) {
            this.timestamp = timestamp;
            this.javaVersion = javaVersion;
        }
    }



    @Override
    public String toString() {
        return this.projectBaseDir.getFileName().toString();
    }

}
