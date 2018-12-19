package org.jerkar.tool;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.JkUrlClassLoader;
import org.jerkar.api.system.JkException;
import org.jerkar.api.system.JkHierarchicalConsoleLogHandler;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsTime;

/**
 * Main class for launching Jerkar from command line.
 *
 * @author Jerome Angibaud
 */
public final class Main {

    /**
     * Entry point for Jerkar application when launched from command-line
     */
    public static void main(String[] args) throws Exception {
        /*
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (!(classLoader instanceof URLClassLoader)) {
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[0],classLoader);
            Thread.currentThread().setContextClassLoader(urlClassLoader);
            Class<?> mainClass = urlClassLoader.loadClass(Main.class.getName());
            Method mainMethod = JkUtilsReflect.getMethod(mainClass, "main", String[].class);
            Thread.currentThread().setContextClassLoader(urlClassLoader);
            mainMethod.invoke(null, (Object) args);
            return;
        }
        */
        final long start = System.nanoTime();
        try {
            Environment.initialize(args);
            JkLog.registerHierarchicalConsoleHandler();
            JkLog.Verbosity verbosity = JkLog.verbosity();
            if (!Environment.standardOptions.logHeaders) {
                JkLog.setVerbosity(JkLog.Verbosity.MUTE);
            } else {
                displayIntro();
            }
            JkInit.displayInfo();
            final Path workingDir = Paths.get("").toAbsolutePath();
            final Engine engine = new Engine(workingDir);
            engine.execute(Environment.commandLine, Environment.standardOptions.runClass, verbosity);
            if (Environment.standardOptions.logHeaders) {
                displayOutro(start);
            }
        } catch (final RuntimeException e) {
            JkHierarchicalConsoleLogHandler.restore();
            if (e instanceof JkException) {
                System.err.println(e.getMessage());
            } else {
                e.printStackTrace(System.err);
            }
            if (Environment.standardOptions.logHeaders) {
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
     * Entry point to call Jerkar on a given folder
     */
    public static void exec(Path projectDir, String... args) {
        final Engine engine = new Engine(projectDir);
        Environment.initialize(args);
        JkLog.Verbosity verbosity = JkLog.verbosity();
        if (!Environment.standardOptions.logHeaders) {
            JkLog.setVerbosity(JkLog.Verbosity.MUTE);
        }
        engine.execute(Environment.commandLine, Environment.standardOptions.runClass, verbosity);
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
        final int length = printAscii(false, "jerkar.ascii");
        JkLog.info(JkUtilsString.repeat(" ", length) + "The 100% Java build tool.\n");
    }

    private static void displayOutro(long startTs) {
        final int length = printAscii(false, "success.ascii");
        System.out.println(JkUtilsString.repeat(" ", length) + "Total run duration : "
                + JkUtilsTime.durationInSeconds(startTs) + " seconds.");
    }

    private Main() {
    }

}
