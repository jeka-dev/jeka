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

package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkInternalClasspathScanner;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompilerToolChain;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;

import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/*
 * Core of the execution engine. Responsible for :
 *     - Parse sources to extract compilation directive and dependencies
 *     - Compile code on the fly
 *     - Resolve command line actions to executable tasks
 *     - Run the requested actions
 */
class Engine {

    private static final String[] PRIVATE_GLOB_PATTERN = new String[]{"**/_*", "_*"};

    private static final JkPathMatcher JAVA_JEKA_SRC_MATCHER = JkPathMatcher.of(true, "**.java")
            .and(false, PRIVATE_GLOB_PATTERN);

    private static final JkPathMatcher JAVA_CLASS_MATCHER = JkPathMatcher.of(true, "**.class")
            .and(false, PRIVATE_GLOB_PATTERN);

    private static final JkPathMatcher KOTLIN_JEKA_SRC_MATCHER = JkPathMatcher.of(true, "**.kt")
            .and(false, PRIVATE_GLOB_PATTERN);

    private static final JkPathMatcher JAVA_DEF_SOURCE_MATCHER = JkPathMatcher.of(true, "**.java")
            .and(false, "**/_*", "_*");

    private static final JkPathMatcher KOTLIN_DEF_SOURCE_MATCHER = JkPathMatcher.of(true, "**.kt")
            .and(false, "**/_*", "_*");

    static final JkPathMatcher JAVA_OR_KOTLIN_SOURCE_MATCHER = JAVA_DEF_SOURCE_MATCHER.or(KOTLIN_DEF_SOURCE_MATCHER);

    private static final String NO_JDK_MSG = String.format(
            "The running Java platform %s is not a valid JDK (no javac found).%n" +
                    "Please provide a JDK by specifying 'jeka.java.version' in jeka.properties file.%n" +
                    "Or set JAVA_HOME environment variable to a valid JDK.", System.getProperty("java.home"));

    private static final Map<Path, Engine> MAP = new HashMap<>();

    final Path baseDir;

    private final boolean isMaster;

    private final JkDependencyResolver dependencyResolver;

    private final JkDependencySet commandLineDependencies;

    // Computed values

    private final JkProperties properties;

    private final Path jekaSrcDir;

    private final Path jekaSrcClassDir;

    private ClasspathSetupResult classpathSetupResult;

    private KBeanResolution kbeanResolution;

    private KBeanAction.Container actionContainer;

    private JkRunbase runbase;

    private Engine(boolean isMaster, Path baseDir,
                   JkRepoSet downloadRepos,
                   JkDependencySet commandLineDependencies) {

        this.baseDir = baseDir;
        this.isMaster = isMaster;
        this.commandLineDependencies = commandLineDependencies;
        this.properties = JkRunbase.constructProperties(baseDir);
        this.jekaSrcDir = baseDir.resolve(JkConstants.JEKA_SRC_DIR);
        this.jekaSrcClassDir = baseDir.resolve(JkConstants.JEKA_SRC_CLASSES_DIR);

        // Set dependency resolver
        this.dependencyResolver = JkDependencyResolver.of(downloadRepos)
                .setUseFileSystemCache(true)
                .setFileSystemCacheDir(baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve("jeka-src-deps"));

        // In lenient mode, we ignore dep resolution failure
        this.dependencyResolver.getDefaultParams().setFailOnDependencyResolutionError(
                !BehaviorSettings.INSTANCE.forceMode);
    }

    static Engine of(boolean isMaster, Path baseDir, JkRepoSet downloadRepos,
                     JkDependencySet commandLineDependencies) {

        Path path = baseDir.toAbsolutePath().normalize();


        // ensure only 1 baseProcessor per base
        return MAP.computeIfAbsent(path,
                key -> new Engine(isMaster, key, downloadRepos, commandLineDependencies));
    }

    private Engine withBaseDir(Path baseDir) {
        return of(false, baseDir, this.dependencyResolver.getRepos(), this.commandLineDependencies);
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof Engine)) return false;

        Engine engine = (Engine) o;
        return baseDir.equals(engine.baseDir);
    }

    @Override
    public int hashCode() {
        return baseDir.hashCode();
    }

    /*
     * Resolves dependencies and compiles and sources classes contained in jeka-src.
     * It returns a path sequence containing the resolved dependencies and result of compilation.
     */
    ClasspathSetupResult resolveClassPaths() {

        if (classpathSetupResult != null) {
            return classpathSetupResult;
        }

        JkLog.debugStartTask("Resolve classpath for jeka-src");

        if (BehaviorSettings.INSTANCE.cleanWork) {
            Path workDir = baseDir.resolve(JkConstants.JEKA_WORK_PATH);
            JkLog.debug("Clean .jaka-work directory  %s ", workDir.toAbsolutePath().normalize());
            JkPathTree.of(workDir).deleteContent();
        }

        // That is nice that this clean occurs here, because it will happen for sub-basedir as well.
        if (BehaviorSettings.INSTANCE.cleanOutput) {
            Path outputDir = baseDir.resolve(JkConstants.OUTPUT_PATH);
            JkPathTree.of(outputDir).deleteContent();
            JkLog.verbose("Clean %s dir", outputDir);
        }

        // Parse info from source code
        JkLog.debugStartTask("Scan jeka-src code for finding dependencies");
        final ParsedSourceInfo parsedSourceInfo = SourceParser.of(this.baseDir).parse();

        JkLog.debug("Imported base dirs:" + parsedSourceInfo.importedBaseDirs);
        JkLog.debugEndTask();

        // Compute and get the classpath from sub-dirs
        List<Engine> subBaseDirs = parsedSourceInfo.importedBaseDirs.stream()
                .map(this::withBaseDir)
                .collect(Collectors.toList());

        JkPathSequence addedClasspathFromSubDirs = subBaseDirs.stream()
                .map(Engine::resolveClassPaths)
                .map(classpathSetupResult -> classpathSetupResult.runClasspath)
                .reduce(JkPathSequence.of(), JkPathSequence::and);

        // compute classpath to look in for finding KBeans
        String classpathProp = properties.get(JkConstants.CLASSPATH_PROP, "");
        if (JkUtilsString.isBlank(classpathProp)) {
            classpathProp = properties.get(JkConstants.CLASSPATH_INJECT_PROP, "");
        }
        List<JkDependency> jekaPropsDeps = Arrays.stream(classpathProp.split(" "))
                .flatMap(desc -> Arrays.stream(desc.split(",")))     // handle ' ' or ',' separators
                .map(String::trim)
                .filter(desc -> !desc.isEmpty())
                .map(desc -> JkDependency.of(baseDir, desc))
                .collect(Collectors.toList());
        JkDependencySet dependencies = commandLineDependencies
                .and(jekaPropsDeps)
                .and(parsedSourceInfo.getDependencies())
                .andVersionProvider(JkConstants.JEKA_VERSION_PROVIDER);

        JkPathSequence kbeanClasspath = JkPathSequence.of(dependencyResolver.resolveFiles(dependencies))
                .and(JkLocator.getJekaJarPath());

        if (BehaviorSettings.INSTANCE.skipCompile) {
            JkLog.debugEndTask();
            this.classpathSetupResult = compileLessClasspathResult(parsedSourceInfo, subBaseDirs, kbeanClasspath, dependencies);
            return this.classpathSetupResult;
        }

        // compute classpath for compiling 'jeka-src'
        JkPathSequence compileClasspath = kbeanClasspath
                .and(addedClasspathFromSubDirs)
                .and(JkPathTree.of(baseDir.resolve(JkConstants.JEKA_BOOT_DIR)).getFiles());

        // Compile jeka-src
        CompileResult compileResult = compileJekaSrc(compileClasspath, parsedSourceInfo.compileOptions);

        // Add the compilation result to the run classpath
        JkPathSequence runClasspath = compileClasspath.and(compileResult.extraClasspath).withoutDuplicates();

        // If private dependencies have been defined, this means that jeka-src is supposed to host
        // an application that wants to control its classpath. Thus, we only put the dependency explicitly
        // exported.
        final JkPathSequence exportedClasspath;
        final JkDependencySet exportedDependencies;
        if (parsedSourceInfo.hasPrivateDependencies()) {
            exportedDependencies = parsedSourceInfo.getExportedDependencies()
                    .andVersionProvider(JkConstants.JEKA_VERSION_PROVIDER);
            exportedClasspath = JkPathSequence.of(
                    dependencyResolver.resolveFiles(parsedSourceInfo.getExportedDependencies()));
        } else {
            exportedDependencies = parsedSourceInfo.getDependencies();
            exportedClasspath = compileClasspath;
        }

        // Prepare result and return
        this.classpathSetupResult = new ClasspathSetupResult(compileResult.success,
                runClasspath, kbeanClasspath, exportedClasspath, exportedDependencies, subBaseDirs, dependencies);


        JkLog.debugEndTask();
        return classpathSetupResult;
    }

    KBeanResolution resolveKBeans() {
        if (kbeanResolution != null) {
            return kbeanResolution;
        }
        if (classpathSetupResult == null) {
            resolveClassPaths();
        }

        // Find all KBean class available for this
        List<String> kbeanClassNames = findKBeanClassNames();
        this.kbeanResolution = KBeanResolution.of(isMaster, this.properties, baseDir, kbeanClassNames);
        return kbeanResolution;
    }

    JkRunbase initRunbase(KBeanAction.Container actionContainer) {
        if (runbase != null) {
            return runbase;
        }
        this.actionContainer = actionContainer;

        runbase = JkRunbase.createMaster(baseDir);
        runbase.setKbeanResolution(getKbeanResolution());
        runbase.setDependencyResolver(dependencyResolver);
        runbase.setClasspath(classpathSetupResult.runClasspath);
        runbase.setExportedClassPath(classpathSetupResult.exportedClasspath);
        runbase.setExportedDependencies(classpathSetupResult.exportedDependencies);
        runbase.setFullDependencies(classpathSetupResult.fullDependencies);
        runbase.assertValid(); // fail-fast. bugfix purpose


        // initialise runbase with resolved commands
        runbase.init(this.actionContainer);
        return runbase;
    }

    void run() {
        runbase.run(actionContainer);
    }

    String defaultKBeanClassName(List<String> kbeanClassNames, List<String> localKbeanClassNames) {
        return DefaultKBeanResolver.get(isMaster, this.properties, kbeanClassNames, localKbeanClassNames);
    }

    JkRunbase getRunbase() {
        return runbase;
    }

    ClasspathSetupResult getClasspathSetupResult() {
        return classpathSetupResult;
    }

    KBeanResolution getKbeanResolution() {
        return kbeanResolution;
    }

    private CompileResult compileJekaSrc(JkPathSequence classpath, List<String> compileOptions) {

        JkPathSequence extraClasspath = JkPathSequence.of();
        boolean hasKotlinSources = hasKotlinSource();

        // Avoid compilation if sources didn't change
        EngineCompilationUpdateTracker updateTracker = new EngineCompilationUpdateTracker(baseDir);
        if (!updateTracker.needRecompile(classpath)) {
            if (hasKotlinSources) {
                JkPathSequence kotlinStdLibPath = updateTracker.readKotlinLibsFile();
                extraClasspath = extraClasspath.and(kotlinStdLibPath);
            }
            extraClasspath = extraClasspath.and(jekaSrcClassDir);
            return new CompileResult(true, extraClasspath);
        }

        // Compile Kotlin code if any
        KotlinCompileResult kotlinCompileResult = new KotlinCompileResult(true, JkPathSequence.of());
        if (hasKotlinSource()) {
            kotlinCompileResult = compileWithKotlin(classpath, compileOptions);
            extraClasspath = extraClasspath.and(kotlinCompileResult.kotlinLibPath);
            updateTracker.updateKotlinLibs(kotlinCompileResult.kotlinLibPath);
        }

        // Compile  Java
        boolean javaCompileSuccess = compileJava(classpath, compileOptions);

        // Copy resources
        if (Files.isDirectory(jekaSrcDir)) {
            JkPathTree.of(jekaSrcDir)
                    .andMatching(false, "**/*.java", "*.java", "**/*.kt", "*.kt")
                    .copyTo(jekaSrcClassDir, StandardCopyOption.REPLACE_EXISTING);
        }

        // Prepare and return result
        updateTracker.updateCompileFlag();
        extraClasspath = extraClasspath.and(jekaSrcClassDir);
        boolean globalSuccess = javaCompileSuccess && kotlinCompileResult.success;
        if (!globalSuccess && !BehaviorSettings.INSTANCE.forceMode) {
            throw new JkException("Compilation of %s failed.", jekaSrcDir);
        }

        return new CompileResult(globalSuccess, extraClasspath);
    }

    private boolean hasKotlinSource() {
        return JkPathTree.of(jekaSrcDir).andMatcher(KOTLIN_JEKA_SRC_MATCHER)
                .count(1, false) > 0;
    }

    private KotlinCompileResult compileWithKotlin(JkPathSequence classpath, List<String> compileOptions) {
        String kotVer = properties.get(JkKotlinCompiler.KOTLIN_VERSION_OPTION);
        if (JkUtilsString.isBlank(kotVer)) {
            String message = String.format( "No Kotlin version has been defined for %s.%n" +
                            " Please, mention 'jeka.kotlin.version=xxx' in %s.",
                    baseDir,
                    baseDir.resolve(JkConstants.PROPERTIES_FILE));
            if (BehaviorSettings.INSTANCE.forceMode) {
                JkLog.warn(message);
                return new KotlinCompileResult(false, JkPathSequence.of());
            }
            throw new IllegalStateException(message);
        }
        JkKotlinCompiler kotlinCompiler = JkKotlinCompiler.ofJvm(dependencyResolver.getRepos(), kotVer)
                .setLogOutput(true)
                .setFailOnError(!BehaviorSettings.INSTANCE.forceMode)
                .addOption("-nowarn");
        compileOptions.forEach(option -> kotlinCompiler.addOption(option));
        JkPathSequence kotlinClasspath = classpath.and(kotlinCompiler.getStdJdk8Lib());
        final JkKotlinJvmCompileSpec kotlinCompileSpec = JkKotlinJvmCompileSpec.of()
                .setClasspath(kotlinClasspath)
                .setSources(JkPathTreeSet.ofRoots(jekaSrcDir))
                .setOutputDir(jekaSrcClassDir);
        if (JkLog.isVerbose()) {
            kotlinCompiler.addOption("-verbose");
        }

        // perform compilation in console spinner
        boolean success = kotlinCompiler.compile(kotlinCompileSpec);

        return new KotlinCompileResult(success, kotlinCompiler.getStdJdk8Lib());
    }

    private boolean compileJava(JkPathSequence classpath, List<String> compileOptions) {
        final JkPathTree jekaSource = JkPathTree.of(jekaSrcDir).andMatcher(JAVA_JEKA_SRC_MATCHER);
        JkJavaCompileSpec javaCompileSpec = JkJavaCompileSpec.of()
                .setClasspath(classpath.and(jekaSrcClassDir)) // for handling compiled kotlin code
                .setOutputDir(jekaSrcClassDir)
                .setSources(jekaSource.toSet())
                .addOptions(compileOptions);
        if (javaCompileSpec.getSources().containFiles() && ToolProvider.getSystemJavaCompiler() == null) {
            throw new JkException(NO_JDK_MSG);
        }
        if (jekaSource.containFiles()) {
            return JkJavaCompilerToolChain.of().compile(javaCompileSpec);
        }
        JkLog.verbose("jeka-src dir does not contain sources. Skip compile");
        return true;
    }

    private List<String> findKBeanClassNames() {

        JkInternalClasspathScanner scanner = JkInternalClasspathScanner.of();

        // Find classes in jeka-src-classes. As it is small scope, we don't need caching
        final List<String> jekaSrcKBeans;
        if (JkPathTree.of(jekaSrcClassDir).withMatcher(JAVA_CLASS_MATCHER).containFiles()
                && !BehaviorSettings.INSTANCE.skipCompile) {

            ClassLoader srcClassloader = JkUrlClassLoader.of(jekaSrcClassDir).get();
            jekaSrcKBeans = scanner.findClassesExtending(srcClassloader, KBean.class, jekaSrcClassDir);
        } else {
            jekaSrcKBeans = Collections.emptyList();
        }

        // Find KBeans in dependencies. We need caching as there are potentially a lot of jars to scan
        JkPathSequence kbeanClasspath = classpathSetupResult.kbeanClasspath;

        // -- Look cached result in files
        Path classpathCache = baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve(JkConstants.KBEAN_CLASSPATH_CACHE_FILE);
        JkPathSequence cachedClasspath = JkPathSequence.readFromQuietly(classpathCache);
        Path kbeanCache = baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve(JkConstants.KBEAN_CLASS_NAMES_CACHE_FILE);

        // -- If cache matches, returns cached resul
        // -- kbeanCache file may not exist is compilation is skipped
        if (cachedClasspath.equals(kbeanClasspath) && Files.exists(kbeanCache)) {
            List<String> cachedKbeans = JkUtilsIterable.readStringsFrom(kbeanCache, "\n");
            if (!cachedKbeans.isEmpty()) {
                List<String> result = new LinkedList<>(jekaSrcKBeans);
                result.addAll(cachedKbeans);
                return result;
            }
        }

        // -- Scan deps
        ClassLoader depClassloader = JkUrlClassLoader.of(classpathSetupResult.kbeanClasspath).get();

        // If we skip jeka-src, we need look in KBean classes in parent classloaders as we are
        // probably relying on IDE classloader.
        boolean ignoreParentClassloaders = !BehaviorSettings.INSTANCE.skipCompile;;
        List<String> depsKBeans = scanner.findClassesExtending(
                depClassloader,
                KBean.class,
                ignoreParentClassloaders,
                true,
                true);

        // -- Update cache
        kbeanClasspath.writeTo(classpathCache);
        JkUtilsIterable.writeStringsTo(kbeanCache, "\n", depsKBeans);

        // Prepare and return result
        List<String> result = new LinkedList<>(jekaSrcKBeans);
        result.addAll(depsKBeans);
        return result;
    }

    static class ClasspathSetupResult {

        final JkPathSequence runClasspath;
        final JkPathSequence kbeanClasspath;
        final JkPathSequence exportedClasspath;
        final JkDependencySet exportedDependencies;
        final boolean compileResult;
        final JkDependencySet fullDependencies;
        final List<Engine> subEngines;

        ClasspathSetupResult(boolean compileResult,
                             JkPathSequence runClasspath,  // The full classpath to run Jeka upon
                             JkPathSequence kbeanClasspath, // The classpath to find in KBeans
                             JkPathSequence exportedClasspath,
                             JkDependencySet exportedDependencies,
                             List<Engine> subEngines,
                             JkDependencySet fullDependencies) {
            this.compileResult = compileResult;
            this.runClasspath = runClasspath;
            this.kbeanClasspath = kbeanClasspath;  // does not contain jeka-src-classes
            this.exportedClasspath = exportedClasspath;
            this.exportedDependencies = exportedDependencies;
            this.subEngines = subEngines;
            this.fullDependencies = fullDependencies;
        }
    }

    // For test-purpose only
    void setKBeanResolution(KBeanResolution kbeanResolution) {
        this.kbeanResolution = kbeanResolution;
    }

    private static class KotlinCompileResult {

        final boolean success;

        final JkPathSequence kotlinLibPath;


        public KotlinCompileResult(boolean success, JkPathSequence kotlinLibPath) {
            this.success = success;
            this.kotlinLibPath = kotlinLibPath;
        }
    }

    private static class CompileResult {

        final boolean success;

        private final JkPathSequence extraClasspath;

        CompileResult(boolean success, JkPathSequence extraClasspath) {
            this.success = success;
            this.extraClasspath = extraClasspath;
        }
    }

    private ClasspathSetupResult compileLessClasspathResult(
            ParsedSourceInfo parsedSourceInfo,
            List<Engine> subBaseDirs,
            JkPathSequence classpath,
            JkDependencySet fullDependencies) {

        return new ClasspathSetupResult(
                true,
                classpath,
                classpath,
                JkPathSequence.of(
                        dependencyResolver.resolveFiles(parsedSourceInfo.getExportedDependencies())),
                parsedSourceInfo.getExportedDependencies()
                        .andVersionProvider(JkConstants.JEKA_VERSION_PROVIDER),
                subBaseDirs,
                fullDependencies);
    }

}
