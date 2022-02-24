package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.system.JkBusyIndicator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkMemoryBufferLogDecorator;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsThrowable;
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
        if (!(Thread.currentThread().getContextClassLoader() instanceof AppendableUrlClassloader)) {
            final URLClassLoader urlClassLoader = new AppendableUrlClassloader();
            Thread.currentThread().setContextClassLoader(urlClassLoader);
            final Object[] argArray = new Object[] {args};
            JkClassLoader.of(urlClassLoader).invokeStaticMethod(false, Main.class.getName(),
                    "main" , argArray);
            return;
        }
        final long start = System.nanoTime();
        try {
            Environment.initialize(args);
            PropertyLoader.load(); // Force static initializer
            JkLog.setDecorator(Environment.standardOptions.logStyle);
            if (Environment.standardOptions.logBanner) {
                displayIntro();
            }
            if (Environment.standardOptions.logRuntimeInformation) {
                JkInit.displayRuntimeInfo();
            }
            JkLog.setAcceptAnimation(!Environment.standardOptions.logNoAnimation);
            if (!Environment.standardOptions.logSetup) {  // log in memory and flush in console only on error
                JkBusyIndicator.start("Preparing Jeka classes and instance (Use -lsu option for details)");
                JkMemoryBufferLogDecorator.activateOnJkLog();
                JkLog.info("");   // To have a br prior the memory log is flushed
            }
            final Path workingDir = Paths.get("");
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
            System.err.println("\nAn error occurred during def class execution.");
            System.err.println("It may come from user code/setting or a bug in Jeka.");
            System.err.println("To investigate, relaunch command with options :");
            System.err.println("    -ls=DEBUG to see code class/line where each log has been emitted.");
            System.err.println("    -lv to increase log verbosity.");
            System.err.println("    -lst to log the full stacktrace of the thrown exception.");
            System.err.println("If error reveals to coming from Jeka engine, please report to " +
                    ": https://github.com/jerkar/jeka/issues");
            System.err.println();
            if (JkLog.isVerbose()
                    || Environment.standardOptions.logStyle == JkLog.Style.DEBUG
                    || Environment.standardOptions.logStackTrace) {
                e.printStackTrace(System.err);
            } else {
                JkUtilsThrowable.printStackTrace(System.err, e, 3);
            }
            if (Environment.standardOptions.logBanner) {
                final int length = printAscii(true, "text-failed.ascii");
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
        JkLog.setAcceptAnimation(!Environment.standardOptions.logNoAnimation);
        if (!Environment.standardOptions.logSetup) {
            JkBusyIndicator.start("Preparing Jeka classes and instance (Use -lsu option for details)");
            JkMemoryBufferLogDecorator.activateOnJkLog();
        }
        final Engine engine = new Engine(projectDir);
        engine.execute(CommandLine.parse(args));
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
        final int length = printAscii(false, "text-jeka.ascii");
        JkLog.info(JkUtilsString.repeat(" ", length) + "The 100%% Java Build Tool.\n");
    }

    private static void displayOutro(long startTs) {
        final int length = printAscii(false, "text-success.ascii");
        System.out.println(JkUtilsString.repeat(" ", length) + "Total run duration : "
                + JkUtilsTime.durationInSeconds(startTs) + " seconds.");
    }

    private Main() {
    }

}
