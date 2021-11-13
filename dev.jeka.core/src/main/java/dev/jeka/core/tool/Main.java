package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.system.JkBusyIndicator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkMemoryBufferLogDecorator;
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
            final URLClassLoader urlClassLoader = new RelaxedUrlClassloader();
            Thread.currentThread().setContextClassLoader(urlClassLoader);
            JkClassLoader.of(urlClassLoader).invokeStaticMethod(false, Main.class.getName(),
                    "main" , args);
            return;
        }
        final long start = System.nanoTime();
        JkUtilsSystem.disableUnsafeWarning();
        try {
            Environment.initialize(args);
            JkLog.setDecorator(Environment.standardOptions.logStyle);
            if (Environment.standardOptions.logBanner) {
                displayIntro();
            }
            if (Environment.standardOptions.logRuntimeInformation != null) {
                JkInit.displayRuntimeInfo();
            }
            if (!Environment.standardOptions.logSetup) {  // log in memory and flush in console only on error
                JkBusyIndicator.start("Preparing Jeka classes and instance (Use -LSU option for details)");
                JkMemoryBufferLogDecorator.activateOnJkLog();
                JkLog.info("");   // To have a br prior the memory log is flushed
            }
            final Path workingDir = Paths.get("").toAbsolutePath();
            final Engine engine = new Engine(workingDir);
            engine.execute(Environment.commandLine);   // log in memory are inactivated inside this method if it goes ok
            if (Environment.standardOptions.logBanner) {
                displayOutro(start);
            }
            System.exit(0); // Triggers shutdown hooks
        } catch (final Throwable e) {
            JkBusyIndicator.stop();
            if (JkMemoryBufferLogDecorator.isActive()) {
                JkMemoryBufferLogDecorator.flush();
            }
            JkLog.restoreToInitialState();

            if (e instanceof JkException) {
                System.err.println();
                System.err.println(e.getMessage());
            } else {
                System.err.println("An error occurred during def class execution.");
                System.err.println("It may come from user code/setting or a bug in Jeka.");
                System.err.println("You can investigate using the stacktrace below or by relaunching the command using option -LS=DEBUG.");
                if (!JkLog.isVerbose()) {
                    System.err.println("You can also increase log verbosity using option -LV.");
                }
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
        if (!Environment.standardOptions.logSetup) {
            JkBusyIndicator.start("Preparing Jeka classes and instance (Use -LSU option for details)");
            JkMemoryBufferLogDecorator.activateOnJkLog();
        }
        engine.execute(Environment.commandLine);
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
