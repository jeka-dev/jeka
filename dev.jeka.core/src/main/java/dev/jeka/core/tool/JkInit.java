package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.system.*;
import dev.jeka.core.api.text.Jk2ColumnsText;
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
        Environment.parsedCmdLine.getSystemProperties().forEach((k, v) -> System.setProperty(k, v));
        JkLog.setDecorator(Environment.logs.style);
        if (Environment.logs.runtimeInformation) {
            displayRuntimeInfo(Paths.get(""), args);
            JkLog.verbose("JeKa Classpath    : ");
            JkClassLoader.ofCurrent().getClasspath().getEntries().forEach(item -> JkLog.verbose("    " + item));
        }
        boolean memoryBufferLogActivated = false;
        if (!JkMemoryBufferLogDecorator.isActive()) {  // log in memory and flush in console only on error
            JkMemoryBufferLogDecorator.activateOnJkLog();
            JkLog.info("");   // To have a br prior the memory log is flushed
            memoryBufferLogActivated = true;
        }
        try {
            EngineKBeanClassResolver engineKBeanClassResolver = new EngineKBeanClassResolver(Paths.get(""));
            List<EngineCommand> commands = new LinkedList<>();

            commands.add(new EngineCommand(EngineCommand.Action.BEAN_INIT, clazz, null, null));
            commands.addAll(engineKBeanClassResolver.resolve(Environment.parsedCmdLine, KBean.name(clazz), false));

            JkRunbase runbase = JkRunbase.get(Paths.get(""));
            runbase.setImportedRunbaseDirs(getImportedProjects(clazz));
            JkProperties properties = JkRunbase.constructProperties(Paths.get(""));
            JkRepoSet repos = JkRepoProperties.of(properties).getDownloadRepos();
            JkDependencyResolver dependencyResolver = JkDependencyResolver.of(repos);
            dependencyResolver.getDefaultParams().setFailOnDependencyResolutionError(true);
            runbase.setDependencyResolver(dependencyResolver);
            runbase.setClasspath(JkPathSequence.of(JkClasspath.ofCurrentRuntime()));
            runbase.init(commands);
            runbase.assertValid();;
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

    static void displayRuntimeInfo(Path baseDir, String[] cmdLine) {
        Jk2ColumnsText txt = Jk2ColumnsText.of(18, 150);
        txt.add("Working Directory", System.getProperty("user.dir"));
        txt.add("Base Directory", baseDir.toAbsolutePath());
        txt.add("Command Line",  String.join(" ", Arrays.asList(cmdLine)));
        txt.add("Java Home",  System.getProperty("java.home"));
        txt.add("Java Version", System.getProperty("java.version") + ", " + System.getProperty("java.vendor"));
        txt.add("Jeka Version",  JkInfo.getJekaVersion());

        if ( embedded(JkLocator.getJekaHomeDir().normalize())) {
            txt.add("Jeka Home", bootDir().normalize() + " ( embedded !!! )");
        } else {
            txt.add("Jeka Home", JkLocator.getJekaHomeDir().normalize());
        }
        txt.add("Jeka User Home", JkLocator.getJekaUserHomeDir().toAbsolutePath().normalize());
        txt.add("Jeka Cache Dir",  JkLocator.getCacheDir().toAbsolutePath().normalize());
        JkProperties properties = JkRunbase.constructProperties(Paths.get(""));
        txt.add("Download Repos", JkRepoProperties.of(properties).getDownloadRepos().getRepos().stream()
                .map(JkRepo::getUrl).collect(Collectors.toList()));
        JkLog.info(txt.toString());
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
        List<Path> paths = JkUtilsReflect.getDeclaredFieldsWithAnnotation(clazz, true).stream()
                .map(field -> field.getAnnotation(JkInjectRunbase.class))
                .filter(Objects::nonNull)
                .map(jkInjectProject -> jkInjectProject.value())
                .map(Paths::get)
                .collect(Collectors.toList());
        return JkPathSequence.of(paths).withoutDuplicates();
    }

}
