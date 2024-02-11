package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.system.*;
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

import static dev.jeka.core.tool.Environment.logs;

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
            Environment.parsedCmdLine.getSystemProperties().forEach((k, v) -> System.setProperty(k, v));
            JkLog.setDecorator(logs.style);
            if (logs.banner) {
                displayIntro();
            }
            if (logs.runtimeInformation) {
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
            if (logs.animation != null) {
                logAnimation = logs.animation;
            }
            JkLog.setAcceptAnimation(logAnimation);

            if (!logs.startUp) {  // log in memory and flush in console only on error
                //JkBusyIndicator.start("Preparing Jeka classes and instance (Use -lsu option for details)");
                //JkMemoryBufferLogDecorator.activateOnJkLog();
                //JkLog.info("");   // To have a br prior the memory log is flushed
            }



            //final Engine engine = new Engine(baseDir);
            //engine.execute(Environment.parsedCmdLine);   // log in memory are inactivated inside this method if it goes ok

            // --- code replace for new engine

            EngineBase engineBase = EngineBase.forLegacy(baseDir,
                    JkDependencySet.of(Environment.parsedCmdLine.getJekaSrcDependencies()));
            JkConsoleSpinner.of("Booting JeKa...").run(engineBase::resolveKBeans);
            engineBase.resolveCommandEngine(Environment.parsedCmdLine.getBeanActions());
            engineBase.initRunbase();

            engineBase.run();

            // ------------

            if (logs.banner) {
                displayOutro(start);
            }
            if (logs.totalDuration && !logs.banner) {
                displayDuration(start);
            }
            System.exit(0); // Triggers shutdown hooks
        } catch (final Throwable e) {
            JkBusyIndicator.stop();
            JkLog.restoreToInitialState();
            if (e instanceof JkException && !shouldPrintExceptionDetails()) {
                System.err.println(e.getMessage());
            } else {
                handleRegularException(e);
            }
            if (logs.banner) {
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
        return logs.verbose || logs.stackTrace;
    }

    static void handleRegularException(Throwable e) {
        System.err.println();
        if (logs.verbose || logs.stackTrace) {
            System.err.println("=============================== Stack Trace =============================================");
            e.printStackTrace(System.err);
            System.err.flush();
            System.err.println("=========================================================================================");
            System.err.println();
        }
        System.err.println(e.getMessage());
        System.err.println("You can investigate using -v, -lst or -ls=DEBUG options.");
        System.err.println("If this originates from a bug, please report the issue at: " +
                "https://github.com/jeka-dev/jeka/issues");
    }

    /**
     * Entry point to call Jeka on a given folder
     */
    public static void exec(Path baseDir, String... args) {
        ClassLoader originalClassloader = Thread.currentThread().getContextClassLoader();
        if (!(originalClassloader instanceof URLClassLoader)) {
            final URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {}, originalClassloader);
            Thread.currentThread().setContextClassLoader(urlClassLoader);
            JkClassLoader.of(urlClassLoader).invokeStaticMethod(false, "dev.jeka.core.tool.Main",
                    "exec" , baseDir, args);
            return;
        }
        ParsedCmdLine parsedCmdLine = ParsedCmdLine.parse(args);
        JkLog.setAcceptAnimation(true);
        if (!logs.startUp) {
            //JkBusyIndicator.start("Preparing Jeka classes and instance (Use -lsu option for details)");
            //JkMemoryBufferLogDecorator.activateOnJkLog();
        }

        EngineBase engineBase = EngineBase.forLegacy(baseDir,
                JkDependencySet.of(Environment.parsedCmdLine.getJekaSrcDependencies()));
        engineBase.resolveCommandEngine(parsedCmdLine.getBeanActions());
        engineBase.initRunbase();
        if (JkMemoryBufferLogDecorator.isActive()) {
            JkBusyIndicator.stop();
            JkMemoryBufferLogDecorator.inactivateOnJkLog();
        }
        engineBase.run();

        //final Engine engine = new Engine(projectDir);
        //engine.execute(parsedCmdLine);
    }

    static int printAscii(boolean error, String fileName) {
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

    static void displayIntro() {
        final int length = printAscii(false, "text-jeka.ascii");
        JkLog.info(JkUtilsString.repeat(" ", length) + "The 100%% Java Build Tool.\n");
    }

    static void displayOutro(long startTs) {
        final int length = printAscii(false, "text-success.ascii");
        System.out.println(JkUtilsString.repeat(" ", length) + "Total run duration : "
                + JkUtilsTime.durationInSeconds(startTs) + " seconds.");
    }

    static void displayDuration(long startTs) {
        System.out.println("\nTotal run duration : " + JkUtilsTime.durationInSeconds(startTs) + " seconds.");
    }

    static String[] filteredArgs(String[] originalArgs) {
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
