package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClasspathScanner;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Class for instantiating builds while displaying meaningful information about environment on console.
 */
public final class JkInit {

    /**
     * Creates an instance of the specified command class and displays information about this class andPrepending environment.
     */
    public static <T extends JkCommandSet> T instanceOf(Class<T> clazz, String... args) {
        JkLog.setHierarchicalConsoleConsumer();
        Environment.initialize(args);
        JkLog.Verbosity verbosity = JkLog.verbosity();
        if (!Environment.standardOptions.logHeaders) {
            JkLog.setVerbosity(JkLog.Verbosity.WARN_AND_ERRORS);
        }
        displayInfo();
        final T jkCommands = JkCommandSet.of(clazz);
        JkLog.info("Jeka commands are ready to be executed.");
        JkLog.setVerbosity(verbosity);
        return jkCommands;
    }

    static void displayInfo() {
        StringBuilder sb = new StringBuilder()
                .append("\nWorking Directory : " + System.getProperty("user.dir"))
                .append("\nJava Home : " + System.getProperty("java.home"))
                .append("\nJava Version : " + System.getProperty("java.version") + ", "
                        + System.getProperty("java.vendor"))
                .append("\nJeka Version : " + JkInfo.getJekaVersion());
        if ( embedded(JkLocator.getJekaHomeDir().normalize())) {
            sb.append("\nJeka Home : " + bootDir().normalize() + " ( embedded !!! )");
        } else {
            sb.append("\nJeka Home : " + JkLocator.getJekaHomeDir());
        }
        sb.append("\nJeka User Home : " + JkLocator.getJekaUserHomeDir().toAbsolutePath().normalize());
        sb.append("\nJeka Def Repositories : " + Engine.repos().toString());
        sb.append("\nJeka Repository Cache : " + JkLocator.getJekaRepositoryCache());
        if (JkLog.isVerbose()) {
            sb.append("\nJeka Classpath : " + JkClassLoader.ofCurrent());
        }
        sb.append("\nCommand Line : " + JkUtilsString.join(Arrays.asList(Environment.commandLine.rawArgs()), " "));
        sb.append(propsAsString("Specified System Properties", Environment.systemProps));
        sb.append("\nStandard Options : " + Environment.standardOptions);
        sb.append(propsAsString("Options", JkOptions.toDisplayedMap(JkOptions.getAll())));
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

    /**
     * This main method is meant to be use by external tools as IDE plugin. From here, all def classes
     * are supposed to be already present in the current classloader.
     */
    public static void main(String[] args) {
        List<String> actualArgs = new LinkedList<>();
        String commandClassName = null;
        for (String arg : args) {
            if (arg.startsWith("-CC=")) {
                commandClassName = arg.substring(4);
            } else {
                actualArgs.add(arg);
            }
        }
        JkUtilsAssert.argument(commandClassName != null,
                "No argument starting with '-CC=' can be found. Cannot determine Command Class");
        Class<JkCommandSet> clazz = JkInternalClasspathScanner.INSTANCE
                .loadClassesHavingNameOrSimpleName(commandClassName, JkCommandSet.class);
        JkUtilsAssert.argument(clazz != null,
                "Command class having name '" + commandClassName + "' cannot be found.");
        String[] argsToPass = actualArgs.toArray(new String[0]);
        JkCommandSet instance = JkInit.instanceOf(clazz, argsToPass);
        CommandLine commandLine = CommandLine.parse(argsToPass);
        for (CommandLine.MethodInvocation methodInvocation : commandLine.getMasterMethods()) {
            if (methodInvocation.isMethodPlugin()) {
                JkPlugin plugin = instance.getPlugins().get(methodInvocation.pluginName);
                JkUtilsReflect.invoke(plugin, methodInvocation.methodName);
            } else {
                JkUtilsReflect.invoke(instance, methodInvocation.methodName);
            }
        }
     }

}
