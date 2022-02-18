package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoFromProperties;
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

import static dev.jeka.core.api.depmanagement.JkRepoFromProperties.getDownloadRepos;

/**
 * Class for instantiating builds while displaying meaningful information about environment on console.
 */
public final class JkInit {

    private JkInit() {
    }

    /**
     * Creates an instance of the specified Jeka class and displays information about this class andPrepending environment.
     */
    public static <T extends JkBean> T instanceOf(Class<T> clazz, String... args) {
        Environment.initialize(args);
        PropertyLoader.load(); // Force static initializer
        if (!Files.isDirectory(Paths.get("jeka")) ) {
            throw new IllegalStateException("The current directory " + Paths.get("").toAbsolutePath()
                    + " does not seem to be a Jeka project as " +
                    "it does not contain a 'jeka' folder.");
        }
        JkLog.setDecorator(Environment.standardOptions.logStyle);
        if (Environment.standardOptions.logRuntimeInformation != null) {
            displayRuntimeInfo();
            JkLog.info("Jeka Classpath : ");
            JkClassLoader.ofCurrent().getClasspath().getEntries().forEach(item -> JkLog.info("    " + item));
        }
        boolean memoryBufferLogActivated = false;
        if (!Environment.standardOptions.logSetup && !JkMemoryBufferLogDecorator.isActive()) {  // log in memory and flush in console only on error
            JkMemoryBufferLogDecorator.activateOnJkLog();
            JkLog.info("");   // To have a br prior the memory log is flushed
            memoryBufferLogActivated = true;
        }
        try {
            EngineBeanClassResolver engineBeanClassResolver = new EngineBeanClassResolver(Paths.get(""));
            List<EngineCommand> commands = new LinkedList<>();
            commands.add(new EngineCommand(EngineCommand.Action.BEAN_INSTANTIATION, clazz, null, null));
            commands.addAll(engineBeanClassResolver.resolve(Environment.commandLine, JkBean.name(clazz)));
            JkRuntime jkRuntime = JkRuntime.get(Paths.get(""));
            jkRuntime.setImportedProjects(getImportedProjects(clazz));
            jkRuntime.setDependencyResolver(JkDependencyResolver.of()
                    .getDefaultParams()
                        .setFailOnDependencyResolutionError(true)
                    .__
                    .addRepos(getDownloadRepos())
                    .addRepos(JkRepo.ofLocal()));
            jkRuntime.setClasspath(JkPathSequence.of(JkClasspath.ofCurrentRuntime()));
            jkRuntime.init(commands);
            final T jkBean = jkRuntime.getBean(clazz);
            JkLog.info(jkBean.toString() + " is ready to run.");
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
    public static <T extends JkBean> T instanceOf(Class<T> clazz, String[] args, String extraArg, String ...extraArgs) {
        String[] allExtraArgs = JkUtilsIterable.concat(new String[] {extraArg}, extraArgs);
        String[] effectiveArgs = JkUtilsIterable.concat(allExtraArgs, args);
        return instanceOf(clazz, effectiveArgs);
    }

    static void displayRuntimeInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nWorking Directory : " + System.getProperty("user.dir"));
        sb.append("\nCommand Line : " + String.join(" ", Arrays.asList(Environment.commandLine.rawArgs())));
        sb.append(propsAsString("Specified properties", PropertyLoader.toDisplayedMap(
                JkProperties.getAllStartingWith("jeka."))));
        sb.append("\nJava Home : " + System.getProperty("java.home"));
        sb.append("\nJava Version : " + System.getProperty("java.version") + ", " + System.getProperty("java.vendor"));
        sb.append("\nJeka Version : " + JkInfo.getJekaVersion());
        if ( embedded(JkLocator.getJekaHomeDir().normalize())) {
            sb.append("\nJeka Home : " + bootDir().normalize() + " ( embedded !!! )");
        } else {
            sb.append("\nJeka Home : " + JkLocator.getJekaHomeDir());
        }
        sb.append("\nJeka User Home : " + JkLocator.getJekaUserHomeDir().toAbsolutePath().normalize());
        sb.append("\nJeka Cache Dir : " + JkLocator.getCacheDir().toAbsolutePath().normalize());
        sb.append("\nJeka download Repositories : " + JkRepoFromProperties.getDownloadRepos());
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
        return Paths.get(JkConstants.BOOT_DIR);
    }

    private static JkPathSequence getImportedProjects(Class<?> clazz) {
        List<Path> paths = JkUtilsReflect.getAllDeclaredFields(clazz, true).stream()
                .map(field -> field.getAnnotation(JkInjectProject.class))
                .filter(Objects::nonNull)
                .map(jkInjectProject -> jkInjectProject.value())
                .map(Paths::get)
                .collect(Collectors.toList());
        return JkPathSequence.of(paths).withoutDuplicates();
    }



}
