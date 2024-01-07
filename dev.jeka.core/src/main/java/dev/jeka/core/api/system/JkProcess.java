package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * The JkProcess class represents a process that can be executed on the system.
 * It provides various methods for configuring and executing the process.
 */
public class JkProcess extends JkAbstractProcess<JkProcess> {

    protected JkProcess() {}

    /**
     * Creates a <code>JkProcess</code> using the specified command and
     * parameters.
     */
    public static JkProcess of(String command, String... parameters) {
        return new JkProcess().setCommand(command).addParams(parameters);
    }

    /**
     * Creates a new {@link JkProcess} object using the specified command line.
     *
     * @param commandLine The command line to be executed. The command line is a space separated string
     *                    that will be parsed in an array od arguments.
     */
    public static JkProcess ofCmdLine(String commandLine) {
        String[] items = JkUtilsString.parseCommandline(commandLine);
        JkUtilsAssert.argument(items.length > 0, "Cannot accept empty command line.");
        String cmd = items[0];
        List<String> args = new LinkedList<>(Arrays.asList(items));
        args.remove(0);
        return of(cmd, args.toArray(new String[0]));
    }

    /**
     * Creates a new {@link JkProcess} object with the appropriate command based on the operating system.
     *
     * @param windowsCommand The command to be executed on Windows.
     * @param unixCommand    The command to be executed on Unix-like systems.
     * @param parameters     The parameters to be passed to the command.
     */
    public static JkProcess ofWinOrUx(String windowsCommand, String unixCommand,
            String... parameters) {
        final String cmd = JkUtilsSystem.IS_WINDOWS ? windowsCommand : unixCommand;
        return of(cmd, parameters);
    }

    /**
     * Defines a <code>JkProcess</code> using the specified tool of the JDK and
     * parameters. An example of JDK tool is 'javac'.
     */
    public static JkProcess ofJavaTool(String javaTool, String... parameters) {
        Path candidate = CURRENT_JAVA_DIR;
        final boolean exist = findTool(candidate, javaTool);
        if (!exist) {
            candidate = CURRENT_JAVA_DIR.getParent().getParent().resolve("bin");
            if (!findTool(candidate, javaTool)) {
                throw new IllegalArgumentException("No tool " + javaTool + " found neither in "
                        + CURRENT_JAVA_DIR + " nor in "
                        + candidate);
            }
        }
        final String command = candidate.toAbsolutePath().normalize().resolve(javaTool).toString();
        return of(command, parameters);
    }

    private static boolean findTool(Path dir, String name) {
        if (!Files.exists(dir)) {
            return false;
        }
        for (final Path file : JkUtilsPath.listDirectChildren(dir)) {
            if (Files.isDirectory(file)) {
                continue;
            }
            if (file.getFileName().toString().equals(name)) {
                return true;
            }
            final String fileToolName = JkUtilsString.substringBeforeLast(file.getFileName().toString(), ".");
            if (fileToolName.equals(name)) {
                return true;
            }
        }
        return false;
    }

}
