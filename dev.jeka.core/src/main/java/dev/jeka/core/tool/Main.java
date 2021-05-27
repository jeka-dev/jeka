package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.api.utils.JkUtilsTime;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main class for launching Jeka from command line.
 *
 * @author Jerome Angibaud
 */
public final class Main {

    /**
     * Entry point for Jeka application when launched from command-line
     */
    public static void main(String[] args) {
        if (!(Thread.currentThread().getContextClassLoader() instanceof URLClassLoader)) {
            final URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {},
                    Thread.currentThread().getContextClassLoader());
            Thread.currentThread().setContextClassLoader(urlClassLoader);
            final Object[] argArray = new Object[] {args};
            JkClassLoader.of(urlClassLoader).invokeStaticMethod(false, "dev.jeka.core.tool.Main", "main" , argArray);
            return;
        }
        final long start = System.nanoTime();
        JkUtilsSystem.disableUnsafeWarning();
        try {
            Environment.initialize(args);
            JkLog.setConsumer(Environment.standardOptions.logStyle);
            final JkLog.Verbosity verbosity = JkLog.verbosity();
            if (Environment.standardOptions.logBanner) {
                displayIntro();
            }
            if (Environment.standardOptions.logRuntimeInformation != null) {
                JkInit.displayRuntimeInfo();
            }
            if (!Environment.standardOptions.logSetup) {
                JkLog.setVerbosity(JkLog.Verbosity.WARN_AND_ERRORS);
            }
            final Path workingDir = Paths.get("").toAbsolutePath();
            final Engine engine = new Engine(workingDir);
            engine.execute(Environment.commandLine, verbosity);
            if (Environment.standardOptions.logBanner) {
                displayOutro(start);
            }
            System.exit(0); // Triggers shutdown hooks
        } catch (final RuntimeException e) {
            JkLog.JkEventLogConsumer consumer = JkLog.getConsumer();
            if (consumer != null) {
                consumer.restore();
            }
            if (e instanceof JkException) {
                System.err.println(e.getMessage());
            } else {
                System.err.println("An error occurred during def class execution : " + e.getMessage());
                System.err.println("This is mostly due to an error in user settings or scripts.");
                System.err.println("You can investigate using the stacktrace below or relaunching the command with option -LS=DEBUG.");
                System.err.println("If error reveals to coming from Jeka engine, please report to " +
                        ": https://github.com/jerkar/jeka/issues");
                System.err.println();
                e.printStackTrace(System.err);
            }
            if (Environment.standardOptions.logBanner) {
                final int length = printAscii(true, "failed.ascii");
                System.err.println(JkUtilsString.repeat(" ", length) + "Total run duration : "
                        + JkUtilsTime.durationInSeconds(start) + " seconds.");
            } else {
                System.err.println("Failed !");
            }
            System.exit(1);
        }
    }

    /**
     * Entry point to call Jeka on a given folder
     */
    public static void exec(Path projectDir, String... args) {
        ClassLoader originalClassloader = Thread.currentThread().getContextClassLoader();
        if (!(originalClassloader instanceof URLClassLoader)) {
            final URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {}, originalClassloader);
            Thread.currentThread().setContextClassLoader(urlClassLoader);
            JkClassLoader.of(urlClassLoader).invokeStaticMethod(false, "dev.jeka.core.tool.Main",
                    "exec" , projectDir, args);
            return;
        }
        final Engine engine = new Engine(projectDir);
        Environment.initialize(args);
        final JkLog.Verbosity verbosity = JkLog.verbosity();
        if (!Environment.standardOptions.logSetup) {
            JkLog.setVerbosity(JkLog.Verbosity.MUTE);
        }
        engine.execute(Environment.commandLine, verbosity);
    }

    private static int printAscii(boolean error, String fileName) {
        final InputStream inputStream = Main.class.getResourceAsStream(fileName);
        final List<String> lines = JkUtilsIO.readAsLines(inputStream);
        int i = 0;
        for (final String line : lines) {
            if (i < line.length()) {
                i = line.length();
            }
            if (error) {
                System.err.println(line);
            } else {
                System.out.println(line);
            }
        }
        return i;
    }

    private static void displayIntro() {
        final int length = printAscii(false, "jeka.ascii");
        JkLog.info(JkUtilsString.repeat(" ", length) + "The 100%% Java Build Tool.\n");
    }

    private static void displayOutro(long startTs) {
        final int length = printAscii(false, "success.ascii");
        System.out.println(JkUtilsString.repeat(" ", length) + "Total run duration : "
                + JkUtilsTime.durationInSeconds(startTs) + " seconds.");
    }

    private Main() {
    }

}
