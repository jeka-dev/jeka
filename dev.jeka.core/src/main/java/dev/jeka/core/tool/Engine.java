package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.system.JkBusyIndicator;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkMemoryBufferLogDecorator;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsReflect;

import javax.tools.ToolProvider;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Supplier;

import static dev.jeka.core.tool.JkRepoFromOptions.getDownloadRepo;

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

    private static final JkPathMatcher JAVA_DEF_SOURCE_MATCHER = JkPathMatcher.of(true,"**.java")
            .and(false, "**/_*", "_*");

    private static final JkPathMatcher KOTLIN_DEF_SOURCE_MATCHER = JkPathMatcher.of(true,"**.kt")
           .and(false, "**/_*", "_*");

    static final JkPathMatcher JAVA_OR_KOTLIN_SOURCE_MATCHER = JAVA_DEF_SOURCE_MATCHER.or(KOTLIN_DEF_SOURCE_MATCHER);


    private final Path projectBaseDir;

    private final JkDependencyResolver dependencyResolver;

    private final EngineBeanClassResolver beanClassResolver;

    Engine(Path baseDir) {
        super();
        this.projectBaseDir = baseDir.normalize();
        this.beanClassResolver = new EngineBeanClassResolver(baseDir);
        this.dependencyResolver = JkDependencyResolver.of().addRepos(getDownloadRepo(), JkRepo.ofLocal());
    }

    /**
     * Execute the specified command line in Jeka engine.
     */
    void execute(CommandLine commandLine) {
        JkLog.startTask("Compile def and initialise KBeans");
        JkDependencySet commandLineDependencies = JkDependencySet.of(commandLine.getDefDependencies());
        JkLog.trace("Add following dependencies to def classpath : " + commandLineDependencies);
        JkPathSequence computedClasspath = resolveAndCompile( new HashSet<>(), true);
        JkUrlClassLoader.ofCurrent().addEntries(computedClasspath);
        JkRuntime.BASE_DIR_CONTEXT.set(projectBaseDir);
        JkRuntime runtime = JkRuntime.ofContextBaseDir();
        runtime.setDependencyResolver(dependencyResolver);
        JkLog.endTask();
        JkLog.info("KBeans are ready to be executed.");
        if (JkMemoryBufferLogDecorator.isActive()) {
            JkBusyIndicator.stop();
            JkMemoryBufferLogDecorator.inactivateOnJkLog();
        }
        if (Environment.standardOptions.logRuntimeInformation != null) {
            JkLog.info("Jeka Classpath : ");
            computedClasspath.iterator().forEachRemaining(item -> JkLog.info("    " + item));
        }
        try {
            Optional<String> defaultBeanHint = Optional.ofNullable(Environment.standardOptions.jkClassName());
            Class<? extends JkBean> defaultJkBeanClass = beanClassResolver.resolveDefaultJkBeanClass(defaultBeanHint);
            JkBean bean = runtime.of(defaultJkBeanClass);
            this.launch(bean, commandLine);
        } catch (final RuntimeException e) {
            JkLog.error("Engine " + projectBaseDir + " failed");
            throw e;
        }
    }

    private CompilationContext preCompile() {
        final List<Path> sourceFiles = JkPathTree.of(beanClassResolver.defSourceDir)
                .andMatcher(JAVA_OR_KOTLIN_SOURCE_MATCHER).getFiles();
        JkLog.trace("Parse source code of " + sourceFiles);
        final EngineSourceParser parser = EngineSourceParser.of(this.projectBaseDir, sourceFiles);
        return new CompilationContext(
                jekaClasspath().and(dependencyResolver.resolve(parser.dependencies()).getFiles()),
                new LinkedList<>(parser.projects()),
                parser.compileOptions()
        );
    }

    /*
     * Resolves dependencies and compiles and sources classes contained in jeka/def.
     * It returns a path sequence containing the resolved dependencies and result of compilation.
     */
    private JkPathSequence resolveAndCompile(Set<Path> yetCompiledProjects, boolean compileSources) {
        if (yetCompiledProjects.contains(this.projectBaseDir)) {
            return JkPathSequence.of();
        }
        yetCompiledProjects.add(this.projectBaseDir);
        CompilationContext compilationContext = preCompile();
        final String msg = "Compiling def classes for project " + this.projectBaseDir.getFileName().toString();
        final long start = System.nanoTime();
        JkLog.startTask(msg);
        List<Path> importedProjectClasspath = new LinkedList<>();
        compilationContext.importedProjectDirs.forEach(importedProjectDir -> {
            Engine importedProjectEngine = new Engine(importedProjectDir);
            importedProjectClasspath.addAll(resolveAndCompile(yetCompiledProjects, compileSources).getEntries());
        });
        JkPathSequence classpath = compilationContext.classpath.and(importedProjectClasspath).withoutDuplicates();
        EngineCompilationUpdateTracker compilationTracker = new EngineCompilationUpdateTracker(projectBaseDir);
        if (compileSources) {
            if (Environment.standardOptions.forceCompile() || compilationTracker.isOutdated()) {
                compileDef(classpath, compilationContext.compileOptions);
                compilationTracker.updateCompileFlag();
            } else {
                JkLog.trace("Last def classes are up-to-date : No need to compile.");
            }
        }
        JkLog.endTask();
        return classpath.andPrepend(beanClassResolver.defClassDir);
    }

    private void compileDef(JkPathSequence defClasspath, List<String> compileOptions) {
        JkPathTree.of(beanClassResolver.defClassDir).deleteContent();
        if (hasKotlin()) {
            JkKotlinCompiler kotlinCompiler = JkKotlinCompiler.ofJvm(dependencyResolver.getRepos())
                    .setLogOutput(true)
                    .addOption("-nowarn");
            compileOptions.forEach(option -> kotlinCompiler.addOption(option));
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
        final JkJavaCompileSpec javaCompileSpec = defJavaCompileSpec(defClasspath, compileOptions);
        if (javaCompileSpec.getSources().containFiles() && ToolProvider.getSystemJavaCompiler() == null) {
            throw new JkException("The running Java platform (" +  System.getProperty("java.home") +
                    ") does not provide compiler (javac). Please provide a JDK java platform by pointing JAVA_HOME" +
                    " or JEKA_JDK environment variable to a JDK directory.");
        }
        wrapCompile(() -> JkJavaCompiler.of().compile(javaCompileSpec));
        JkPathTree.of(this.beanClassResolver.defSourceDir)
                .andMatching(false, "**/*.java", "*.java", "**/*.kt", "*.kt")
                .copyTo(this.beanClassResolver.defClassDir, StandardCopyOption.REPLACE_EXISTING);
    }

    private JkPathSequence jekaClasspath() {

        // If true, we assume Jeka is provided in IDE classpath (development mode)
        final boolean devMode = Files.isDirectory(JkLocator.getJekaJarPath());
        JkPathSequence result = JkPathSequence.of(bootLibs());
        if (devMode) {
            result = result.and(JkClasspath.ofCurrentRuntime());
            result = result.and(JkLocator.getJekaJarPath());
        }
        return result.withoutDuplicates();
    }

    private JkPathSequence bootLibs() {
        final List<Path>  extraLibs = new LinkedList<>();
        final Path bootDir = this.projectBaseDir.resolve(JkConstants.BOOT_DIR);
        if (Files.exists(bootDir)) {
            extraLibs.addAll(JkPathTree.of(bootDir).andMatching(true,"**.jar").getFiles());
        }
        return JkPathSequence.of(extraLibs);
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
        return JkPathTree.of(beanClassResolver.defSourceDir).andMatcher(KOTLIN_DEF_SOURCE_MATCHER)
                .count(1, false) > 0;
    }

    private JkJavaCompileSpec defJavaCompileSpec(JkPathSequence classpath, List<String> options) {
        final JkPathTree defSource = JkPathTree.of(beanClassResolver.defSourceDir).andMatcher(JAVA_DEF_SOURCE_MATCHER);
        JkUtilsPath.createDirectories(beanClassResolver.defClassDir);
        return JkJavaCompileSpec.of()
                .setClasspath(classpath.and(beanClassResolver.defClassDir))
                .setOutputDir(beanClassResolver.defClassDir)
                .setSources(defSource.toSet())
                .addOptions(options);
    }

    private JkKotlinJvmCompileSpec defKotlinCompileSpec(JkPathSequence defClasspath) {
        JkUtilsPath.createDirectories(beanClassResolver.defClassDir);
        return JkKotlinJvmCompileSpec.of()
                .setClasspath(defClasspath)
                .setSources(JkPathTreeSet.of(beanClassResolver.defSourceDir))
                .setOutputDir(JkUtilsPath.relativizeFromWorkingDir(beanClassResolver.defClassDir));
    }

    private static void runProject(JkBean jkBean, List<CommandLine.MethodInvocation> invokes) {
        for (final CommandLine.MethodInvocation methodInvocation : invokes) {
            invokeMethodOnCommandSetOrPlugin(jkBean, methodInvocation);
        }
    }

    private static void invokeMethodOnCommandSetOrPlugin(JkBean jkBean, CommandLine.MethodInvocation methodInvocation) {
        if (methodInvocation.beanName != null) {
            final JkBean invokedJkBean = jkBean.getRuntime().getBeanRegistry().get(methodInvocation.beanName);
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

    @Override
    public String toString() {
        return this.projectBaseDir.getFileName().toString();
    }

    private static class CompilationContext {

        private final JkPathSequence classpath;

        private final List<Path> importedProjectDirs;

        private final List<String> compileOptions;

        CompilationContext(JkPathSequence classpath, List<Path> importedProjectDirs,
                           List<String> compileOptions) {
            this.classpath = classpath;
            this.importedProjectDirs = Collections.unmodifiableList(importedProjectDirs);
            this.compileOptions = Collections.unmodifiableList(compileOptions);
        }
    }

}
