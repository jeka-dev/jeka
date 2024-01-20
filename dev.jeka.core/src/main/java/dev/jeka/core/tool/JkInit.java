package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.system.*;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Class for instantiating builds while displaying meaningful information about environment on console.
 */
public final class JkInit {

    private JkInit() {
    }

    /**
     * Creates an instance of the specified Jeka class and displays information about this class andPrepending environment.
     */
    public static <T extends KBean> T instanceOf(Class<T> clazz, String... args) {
        Environment.initialize(args);
        Environment.commandLine.getSystemProperties().forEach((k,v) -> System.setProperty(k, v));
        JkLog.setDecorator(Environment.standardOptions.logStyle);
        if (Environment.standardOptions.logRuntimeInformation) {
            displayRuntimeInfo();
            JkLog.info("JeKa Classpath : ");
            JkClassLoader.ofCurrent().getClasspath().getEntries().forEach(item -> JkLog.info("    " + item));
        }
        boolean memoryBufferLogActivated = false;
        if (!Environment.standardOptions.logSetup && !JkMemoryBufferLogDecorator.isActive()) {  // log in memory and flush in console only on error
            JkMemoryBufferLogDecorator.activateOnJkLog();
            JkLog.info("");   // To have a br prior the memory log is flushed
            memoryBufferLogActivated = true;
        }
        try {
            EngineKBeanClassResolver engineKBeanClassResolver = new EngineKBeanClassResolver(Paths.get(""));
            List<EngineCommand> commands = new LinkedList<>();
            commands.add(new EngineCommand(EngineCommand.Action.BEAN_INSTANTIATION, clazz, null, null));
            commands.addAll(engineKBeanClassResolver.resolve(Environment.commandLine, KBean.name(clazz), false));
            JkRunbase runbase = JkRunbase.get(Paths.get(""));
            runbase.setImportedRunbaseDirs(getImportedProjects(clazz));
            JkProperties properties = JkRunbase.constructProperties(Paths.get(""));
            JkRepoSet repos = JkRepoProperties.of(properties).getDownloadRepos();
            JkDependencyResolver dependencyResolver = JkDependencyResolver.of(repos);
            dependencyResolver.getDefaultParams().setFailOnDependencyResolutionError(true);
            runbase.setDependencyResolver(dependencyResolver);
            runbase.setClasspath(JkPathSequence.of(JkClasspath.ofCurrentRuntime()));
            runbase.init(commands);
            final T jkBean = runbase.load(clazz);
            JkLog.info(jkBean + " is ready to run.");
            if (memoryBufferLogActivated) {
                JkMemoryBufferLogDecorator.inactivateOnJkLog();
            }
            return jkBean;
        } catch (RuntimeException e) {
            if (memoryBufferLogActivated) {
                JkMemoryBufferLogDecorator.flush();
                JkMemoryBufferLogDecorator.inactivateOnJkLog();
            }
            throw e;
        }

    }

    /**
     * Convenient method to let the user add extra arguments.
     * @see #instanceOf(Class, String...)
     */
    public static <T extends KBean> T instanceOf(Class<T> clazz, String[] args, String extraArg, String ...extraArgs) {
        String[] allExtraArgs = JkUtilsIterable.concat(new String[] {extraArg}, extraArgs);
        String[] effectiveArgs = JkUtilsIterable.concat(allExtraArgs, args);
        return instanceOf(clazz, effectiveArgs);
    }

    public static <T extends KBean> T exec(Class<T> clazz, String ...args) {
        T bean = instanceOf(clazz, args);
        return bean;
    }

    static void displayRuntimeInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nWorking Directory : " + System.getProperty("user.dir"));
        sb.append("\nCommand Line      : " + String.join(" ", Arrays.asList(Environment.commandLine.rawArgs())));
        sb.append("\nJava Home         : " + System.getProperty("java.home"));
        sb.append("\nJava Version      : " + System.getProperty("java.version") + ", " + System.getProperty("java.vendor"));
        sb.append("\nJeka Version      : " + JkInfo.getJekaVersion());
        if ( embedded(JkLocator.getJekaHomeDir().normalize())) {
            sb.append("\nJeka Home         : " + bootDir().normalize() + " ( embedded !!! )");
        } else {
            sb.append("\nJeka Home         : " + JkLocator.getJekaHomeDir().normalize());
        }
        sb.append("\nJeka User Home    : " + JkLocator.getJekaUserHomeDir().toAbsolutePath().normalize());
        sb.append("\nJeka Cache Dir    : " + JkLocator.getCacheDir().toAbsolutePath().normalize());
        JkProperties properties = JkRunbase.constructProperties(Paths.get(""));
        sb.append("\nDownload Repos    : " + JkRepoProperties.of(properties).getDownloadRepos().getRepos().stream()
                .map(JkRepo::getUrl).collect(Collectors.toList()));
        sb.append("\nProperties        :\n").append(properties.toKeyValueString("  "));
        JkLog.info(sb.toString());
    }

    private static String propsAsString(String message, Map<String, String> props) {
        StringBuilder sb = new StringBuilder();
        if (props.isEmpty()) {
            sb.append("\n" + message + " : none.");
        } else if (props.size() <= 3) {
            sb.append("\n" + message + " : " + JkUtilsIterable.toString(props));
        } else {
            sb.append("\n" + message + " : ");
            JkUtilsIterable.toStrings(props).forEach(line -> sb.append("  " + line));
        }
        return sb.toString();
    }

    private static boolean embedded(Path jarFolder) {
        if (!Files.exists(bootDir())) {
            return false;
        }
        return JkUtilsPath.isSameFile(bootDir(), jarFolder);
    }

    private static Path bootDir() {
        return Paths.get(JkConstants.JEKA_BOOT_DIR);
    }

    private static JkPathSequence getImportedProjects(Class<?> clazz) {
        List<Path> paths = JkUtilsReflect.getAllDeclaredFields(clazz, true).stream()
                .map(field -> field.getAnnotation(JkInjectRunbase.class))
                .filter(Objects::nonNull)
                .map(jkInjectProject -> jkInjectProject.value())
                .map(Paths::get)
                .collect(Collectors.toList());
        return JkPathSequence.of(paths).withoutDuplicates();
    }



}
