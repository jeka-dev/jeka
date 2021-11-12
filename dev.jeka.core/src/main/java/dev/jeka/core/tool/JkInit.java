package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClasspathScanner;
import dev.jeka.core.api.system.*;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsReflect;

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
     * Creates an instance of the specified Jeka class and displays information about this class andPrepending environment.
     */
    public static <T extends JkClass> T instanceOf(Class<T> clazz, String... args) {
        Environment.initialize(args);
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
            final T jkClass = JkClass.of(clazz);
            JkLog.info(jkClass.toString() + " is ready to run.");
            if (memoryBufferLogActivated) {
                JkMemoryBufferLogDecorator.inactivateOnJkLog();
            }
            return jkClass;
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
    public static <T extends JkClass> T instanceOf(Class<T> clazz, String[] args, String extraArg, String ...extraArgs) {
        String[] allExtraArgs = JkUtilsIterable.concat(new String[] {extraArg}, extraArgs);
        String[] effectiveArgs = JkUtilsIterable.concat(allExtraArgs, args);
        return instanceOf(clazz, effectiveArgs);
    }

    static void displayRuntimeInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nWorking Directory : " + System.getProperty("user.dir"));
        sb.append("\nCommand Line : " + String.join(" ", Arrays.asList(Environment.commandLine.rawArgs())));
        sb.append(propsAsString("Specified System Properties", Environment.systemProps));
        sb.append(propsAsString("Specified Options", JkOptions.toDisplayedMap(JkOptions.getAll())));
        sb.append("\nJava Home : " + System.getProperty("java.home"));
        sb.append("\nJava Version : " + System.getProperty("java.version") + ", " + System.getProperty("java.vendor"));
        sb.append("\nJeka Version : " + JkInfo.getJekaVersion());
        if ( embedded(JkLocator.getJekaHomeDir().normalize())) {
            sb.append("\nJeka Home : " + bootDir().normalize() + " ( embedded !!! )");
        } else {
            sb.append("\nJeka Home : " + JkLocator.getJekaHomeDir());
        }
        sb.append("\nJeka User Home : " + JkLocator.getJekaUserHomeDir().toAbsolutePath().normalize());
        sb.append("\nJeka Def Repositories : " + Engine.repos());
        sb.append("\nJeka Repository Cache : " + JkLocator.getJekaRepositoryCache());
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
        String jkClassName = null;
        for (String arg : args) {
            if (arg.startsWith("-CC=")) {
                jkClassName = arg.substring(4);
            } else {
                actualArgs.add(arg);
            }
        }
        JkUtilsAssert.argument(jkClassName != null,
                "No argument starting with '-CC=' can be found. Cannot determine Jeka Class");
        Class<JkClass> clazz = JkInternalClasspathScanner.INSTANCE
                .loadClassesHavingNameOrSimpleName(jkClassName, JkClass.class);
        JkUtilsAssert.argument(clazz != null,
                "Jeka class having name '" + jkClassName + "' cannot be found.");
        String[] argsToPass = actualArgs.toArray(new String[0]);
        JkClass instance = JkInit.instanceOf(clazz, argsToPass);
        CommandLine commandLine = CommandLine.parse(argsToPass);
        try {
            for (CommandLine.MethodInvocation methodInvocation : commandLine.getMasterMethods()) {
                if (methodInvocation.isMethodPlugin()) {
                    JkBean plugin = instance.getJkBeanRegistry().get(methodInvocation.pluginName);
                    JkUtilsReflect.invoke(plugin, methodInvocation.methodName);
                } else {
                    JkUtilsReflect.invoke(instance, methodInvocation.methodName);
                }
            }
            System.exit(0); // Triggers shutdown hooks
        } catch (Throwable t) {
            System.exit(1); // Triggers shutdown hooks
        }

     }

}
