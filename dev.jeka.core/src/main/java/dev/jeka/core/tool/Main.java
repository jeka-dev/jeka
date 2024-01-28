package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.system.JkBusyIndicator;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkMemoryBufferLogDecorator;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsTime;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static dev.jeka.core.tool.Environment.standardOptions;

/**
 * Main class for launching Jeka from command line.
 *
 * @author Jerome Angibaud
 */
public final class Main {

    private static final String REMOTE_OPTION = "-r";

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
            String[] filteredArgs = filteredArgs(args);
            Environment.initialize(filteredArgs);
            if (Environment.isPureVersionCmd()) {
                System.out.println(JkInfo.getJekaVersion());
                return;
            }
            Environment.commandLine.getSystemProperties().forEach((k,v) -> System.setProperty(k, v));
            JkLog.setDecorator(standardOptions.logStyle);
            if (standardOptions.logBanner) {
                displayIntro();
            }
            if (standardOptions.logRuntimeInformation) {
                JkInit.displayRuntimeInfo();
            }
            String basedirProp = System.getProperty("jeka.current.basedir");
            //System.out.println("---- prop jeka.current.basedir : " + basedirProp);
            final Path baseDir = basedirProp == null ? Paths.get("")
                    : Paths.get("").toAbsolutePath().normalize().relativize(Paths.get(basedirProp));
            //System.out.println("---- actual base dir : " + baseDir);
            //System.out.println("---- actual base dir absolute : " + baseDir.toAbsolutePath().normalize());

            // By default, log working animation when working dir = base dir (this mean that we are not
            // invoking a tool packaged with JeKa.
            boolean logAnimation = baseDir.equals(Paths.get(""));
            if (standardOptions.logAnimation != null) {
                logAnimation = standardOptions.logAnimation;
            }
            JkLog.setAcceptAnimation(logAnimation);

            if (!standardOptions.logStartUp) {  // log in memory and flush in console only on error
                JkBusyIndicator.start("Preparing Jeka classes and instance (Use -lsu option for details)");
                JkMemoryBufferLogDecorator.activateOnJkLog();
                JkLog.info("");   // To have a br prior the memory log is flushed
            }
            final Engine engine = new Engine(baseDir);
            engine.execute(Environment.commandLine);   // log in memory are inactivated inside this method if it goes ok
            if (standardOptions.logBanner) {
                displayOutro(start);
            }
            if (standardOptions.logDuration && !standardOptions.logBanner) {
                displayDuration(start);
            }
            System.exit(0); // Triggers shutdown hooks
        } catch (final Throwable e) {
            JkBusyIndicator.stop();
            JkLog.restoreToInitialState();
            if (e instanceof JkException && !shouldPrintExceptionDetails()) {
                System.err.println(e.getMessage());
            } else {
                if (JkMemoryBufferLogDecorator.isActive()) {
                    JkMemoryBufferLogDecorator.flush();
                }
                handleRegularException(e);
            }
            if (standardOptions.logBanner) {
                final int length = printAscii(true, "text-failed.ascii");
                System.err.println(JkUtilsString.repeat(" ", length) + "Total run duration : "
                        + JkUtilsTime.durationInSeconds(start) + " seconds.");
            } else {
                System.err.println("Failed !");
            }
            System.exit(1);
        }
    }

    private static boolean shouldPrintExceptionDetails() {
        return standardOptions.logVerbose
                || standardOptions.logStackTrace;
    }

    private static void handleRegularException(Throwable e) {
        System.err.println();
        if (JkLog.isVerbose() || standardOptions.logStackTrace) {
            System.err.println("=============================== Stack Trace =============================================");
            e.printStackTrace(System.err);
            System.err.flush();
            System.err.println("=========================================================================================");
            System.err.println();
        }
        System.err.println("An error occurred during execution : " + e.getMessage());
        System.err.println("This could be caused by issues in the user code or settings, or potentially a bug in Jeka.");
        System.err.println("To investigate, relaunch command with options :");
        System.err.println("    -ls=DEBUG to see code class/line where each log has been emitted.");
        if (!JkLog.isVerbose()) {
            System.err.println("    -lv to increase log verbosity.");
            if (!standardOptions.logStackTrace) {
                System.err.println("    -lst to log the full stacktrace of the thrown exception.");
            }
        }
        System.err.println("If error reveals to coming from Jeka engine, please report it to " +
                ": https://github.com/jerkar/jeka/issues");
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
        CommandLine commandLine = CommandLine.parse(args);
        JkLog.setAcceptAnimation(true);
        if (!standardOptions.logStartUp) {
            JkBusyIndicator.start("Preparing Jeka classes and instance (Use -lsu option for details)");
            JkMemoryBufferLogDecorator.activateOnJkLog();
        }
        final Engine engine = new Engine(projectDir);
        engine.execute(commandLine);
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

    private static void displayDuration(long startTs) {
        System.out.println("\nTotal run duration : " + JkUtilsTime.durationInSeconds(startTs) + " seconds.");
    }

    private static String[] filteredArgs(String[] originalArgs) {
        //System.out.println("=====original args = +" + Arrays.asList(originalArgs));
        if (originalArgs.length == 0) {
            return originalArgs;
        }
        List<String> result = new LinkedList<>(Arrays.asList(originalArgs));
        String first =result.get(0);
        if (JkUtilsIterable.listOf("-r", "-rc").contains(first)) {
            result.remove(0);
            result.remove(0);
        } else if (first.startsWith("@")) {   // remove remote @alias
            result.remove(0);
        }
        //System.out.println("=====filterd args = +" + result);
        return result.toArray(new String[0]);
    }

    private Main() {
    }

}
