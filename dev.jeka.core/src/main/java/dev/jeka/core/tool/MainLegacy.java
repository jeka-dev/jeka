package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.system.*;
import dev.jeka.core.api.text.Jk2ColumnsText;
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
import java.util.stream.Collectors;

import static dev.jeka.core.tool.Environment.logs;

/**
 * Main class for launching Jeka from command line.
 *
 * @author Jerome Angibaud
 */
public final class MainLegacy {

    private static final String REMOTE_OPTION = "-r";

    /**
     * Entry point for Jeka application when launched from command-line
     */
    public static void main(String[] args) {
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

            String basedirProp = System.getProperty("jeka.current.basedir");

            final Path baseDir = basedirProp == null ? Paths.get("")
                    : Paths.get("").toAbsolutePath().normalize().relativize(Paths.get(basedirProp));

            if (logs.runtimeInformation) {
                Main.displayRuntimeInfo(baseDir, filteredArgs);
            }

            // By default, log working animation when working dir = base dir (this mean that we are not
            // invoking a tool packaged with JeKa.
            boolean logAnimation = baseDir.equals(Paths.get(""));
            if (logs.animation != null) {
                logAnimation = logs.animation;
            }
            JkLog.setAcceptAnimation(logAnimation);

            JkLog.setShowTaskDuration(logs.duration);

            EngineBase engineBase = EngineBase.forLegacy(baseDir,
                    JkDependencySet.of(Environment.parsedCmdLine.getJekaSrcDependencies()));
            JkConsoleSpinner.of("Booting JeKa...").run(engineBase::resolveKBeans);
            if (logs.runtimeInformation) {
                JkLog.info(Jk2ColumnsText.of(18, 150)
                        .add("Init KBean", engineBase.resolveKBeans().initKBeanClassname)
                        .add("Default KBean", engineBase.resolveKBeans().defaultKbeanClassname)
                        .toString());
                JkProperties properties = JkRunbase.constructProperties(Paths.get(""));
                JkLog.info("Properties         :");
                JkLog.info(properties.toColumnText(30, 90, !JkLog.isVerbose())
                                .setMarginLeft("   | ")
                                .setSeparator(" | ").toString());

                JkPathSequence cp = engineBase.getClasspathSetupResult().runClasspath;
                JkLog.info("Jeka Classpath     :");
                cp.forEach(entry -> JkLog.info("   | " + entry));
            }

            // Change current classloader as we need to have deps and compiled jeka-src class in.
            ClassLoader augmentedClassloader = JkUrlClassLoader.of(engineBase.resolveClassPaths().runClasspath).get();
            Thread.currentThread().setContextClassLoader(augmentedClassloader);

            List<EngineCommand> engineCommands =
                    engineBase.resolveEngineCommand(Environment.parsedCmdLine.getBeanActions());
            if (logs.runtimeInformation) {
                JkLog.info("Commands           :");
                JkLog.info(EngineCommand.toColumnText(engineCommands)
                        .setSeparator(" | ")
                        .setMarginLeft("   | ")
                        .toString());
            }
            engineBase.initRunbase();

            if (logs.runtimeInformation) {
                List<String> beanNames = engineBase.getRunbase().getBeans().stream()
                        .map(Object::getClass)
                        .map(KBean::name)
                        .collect(Collectors.toList());
                JkLog.info("Involved KBeans    :", beanNames);
                JkLog.info("    " + String.join(", ", beanNames));
               JkLog.info("");
            }

            engineBase.run();

            if (logs.banner) {
                displayOutro(start);
            }
            if (logs.duration && !logs.banner) {
                displayDuration(start);
            }
            System.exit(0); // Triggers shutdown hooks
        } catch (final Throwable e) {
            JkBusyIndicator.stop();
            if (e.getMessage() != null) {
                JkLog.error(e.getMessage());
            }
            System.err.println("You can investigate using --verbose, --debug, --stacktrace or -ls=DEBUG options.");
            System.err.println("If this originates from a bug, please report the issue at: " +
                    "https://github.com/jeka-dev/jeka/issues");
            JkLog.restoreToInitialState();
            if ( (!(e instanceof JkException)) || shouldPrintExceptionDetails()) {
                printException(e);
            }
            if (logs.banner) {
                final int length = printAscii(true, "text-failed.ascii");
                System.err.println(JkUtilsString.repeat(" ", length) + "Total run duration : "
                        + JkUtilsTime.formatMillis(System.currentTimeMillis() - start));
            } else {
                System.err.println("Failed !");
            }
            System.exit(1);
        }
    }

    private static boolean shouldPrintExceptionDetails() {
        return logs.debug || logs.stackTrace;
    }

    static void printException(Throwable e) {
        System.err.println();
        if (logs.verbose || logs.stackTrace) {
            System.err.println("=============================== Stack Trace =============================================");
            e.printStackTrace(System.err);
            System.err.flush();
            System.err.println("=========================================================================================");
        }
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

        EngineBase engineBase = EngineBase.forLegacy(baseDir,
                JkDependencySet.of(Environment.parsedCmdLine.getJekaSrcDependencies()));
        engineBase.resolveEngineCommand(parsedCmdLine.getBeanActions());
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
        final InputStream inputStream = MainLegacy.class.getResourceAsStream(fileName);
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
                + JkUtilsTime.formatMillis(System.currentTimeMillis() - startTs));
    }

    static void displayDuration(long startTs) {
        System.out.println("\nTotal run duration : " + JkUtilsTime.formatMillis(System.currentTimeMillis() - startTs) );
    }

    static String[] filteredArgs(String[] originalArgs) {
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

    private MainLegacy() {
    }

}
