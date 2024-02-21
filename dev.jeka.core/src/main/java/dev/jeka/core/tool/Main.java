package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.system.*;
import dev.jeka.core.api.text.Jk2ColumnsText;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsTime;
import dev.jeka.core.tool.CommandLine.Model.CommandSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {

        // Remove -r arguments sent by shell script
        String[] filteredArgs = new CmdLineArgs(args).withoutShellArgs().get();

        // Get the code base directory sent by script shell
        String basedirProp = System.getProperty("jeka.current.basedir");
        Path workingDir = Paths.get("");
        Path baseDir = basedirProp == null ? workingDir : Paths.get(basedirProp);
        if (baseDir.startsWith(workingDir)) {
            baseDir = workingDir.relativize(baseDir);
        }
        exec(baseDir, filteredArgs);
    }

    // TODO Make non-public. Replace by a call creating a new OS process
    public static JkRunbase exec(Path baseDir, String ...args) {

        long startTime = System.currentTimeMillis();

        CmdLineArgs cmdArgs = new CmdLineArgs(args);

        // Handle --help
        // It needs to be fast and safe. Only loads KBeans found in current classpath
        if (cmdArgs.isUsageHelpRequested()) {
            PicocliHelp.printUsageHelp(System.out);
            System.exit(0);
        }

        // Handle --version
        if (cmdArgs.isVersionHelpRequested()) {
            PicocliHelp.printVersionHelp(System.out);
            System.exit(0);
        }

        // Interpolate command line with values found in properties
        JkProperties props = dev.jeka.core.tool.JkRunbase.constructProperties(baseDir);
        CmdLineArgs interpolatedArgs = cmdArgs.interpolated(props);

        LogSettings logs = LogSettings.ofDefault();

        Engine engine = null;

        try {

            // first, parse only options
            PicocliMainCommand mainCommand = new PicocliMainCommand();
            CommandLine commandLine = new CommandLine(CommandSpec.forAnnotatedObject(mainCommand));
            commandLine.parseArgs(interpolatedArgs.withOptionsOnly().get());
            logs = mainCommand.logSettings();
            BehaviorSettings behavior = mainCommand.behaviorSettings();
            JkDependencySet dependencies = mainCommand.dependencies();

            // setup logging
            setupLogging(logs, baseDir, interpolatedArgs.get());

            // Instantiate the Engine
            JkRepoSet downloadRepos = JkRepoProperties.of(props).getDownloadRepos();
            engine = Engine.of(baseDir, behavior.skipCompile,
                    downloadRepos, dependencies, logs, behavior);

            // Compile jeka-src and resolve its dependencies
            JkConsoleSpinner.of("Booting JeKa...").run(engine::resolveKBeans);
            if (logs.runtimeInformation) {
                logRuntimeInfoBase(engine, props);
            }

            // Resolve KBeans
            Engine.KBeanResolution kBeanResolution = engine.getKbeanResolution();
            Engine.ClasspathSetupResult classpathSetupResult = engine.getClasspathSetupResult();

            // Augment current classloader with resolved deps and compiled classes
            ClassLoader augmentedClassloader = JkUrlClassLoader.of(classpathSetupResult.runClasspath).get();
            Thread.currentThread().setContextClassLoader(augmentedClassloader);

            // Handle context help (--commands)
            if (behavior.commandHelp.isPresent()) {
                String kbeanName = behavior.commandHelp.get();
                if (JkUtilsString.isBlank(kbeanName)) {
                    PicocliHelp.printCmdHelp(
                                    engine.resolveClassPaths().runClasspath,
                                    kBeanResolution.allKbeans,
                                    kBeanResolution.defaultKbeanClassname, props, System.out);
                    System.exit(0);
                }
                boolean found = PicocliHelp.printKBeanHelp(
                        engine.resolveClassPaths().runClasspath,
                        kBeanResolution.allKbeans,
                        kbeanName, System.out);
                System.exit(found ? 0 : 1);
            }

            // Parse command line to get action beans
            JkProperties jekaProps = dev.jeka.core.tool.JkRunbase.readBaseProperties(baseDir);
            KBeanAction.Container actionContainer = PicocliParser.parse(
                    interpolatedArgs.withoutOptions(),
                    jekaProps,
                    kBeanResolution);

            // Prepend the init bean in action container
            kBeanResolution.findInitBeanClass().ifPresent(actionContainer::addInitBean);

            if (logs.runtimeInformation) {
                logRuntimeInfoEngineCommands(actionContainer);
            }

            // Run
            engine.initRunbase(actionContainer);
            if (logs.runtimeInformation) {
                logRuntimeInfoRun(engine.getRunbase());
            }
            engine.run();

            logOutro(logs, startTime);

        } catch (CommandLine.ParameterException e) {
            JkBusyIndicator.stop();
            String errorTxt = CommandLine.Help.Ansi.AUTO.string("@|red ERROR: |@");
            CommandLine commandLine = e.getCommandLine();
            commandLine.getErr().println(errorTxt + e.getMessage());
            commandLine.getErr().println("Try 'jeka --commands' or 'jeka -cmd' for more information.");
            System.exit(1);
        } catch (Throwable t) {
            handleGenericThrowable(t, startTime, logs);
            System.exit(1);
        }
        return engine.getRunbase();
    }

    static void displayRuntimeInfo(Path baseDir, String[] cmdLine) {
        Jk2ColumnsText txt = Jk2ColumnsText.of(18, 150);
        txt.add("Working Directory", System.getProperty("user.dir"));
        txt.add("Base Directory", baseDir);
        txt.add("Command Line",  String.join(" ", Arrays.asList(cmdLine)));
        txt.add("Java Home",  System.getProperty("java.home"));
        txt.add("Java Version", System.getProperty("java.version") + ", " + System.getProperty("java.vendor"));
        txt.add("Jeka Version",  JkInfo.getJekaVersion());

        if ( embedded(JkLocator.getJekaHomeDir().normalize())) {
            txt.add("Jeka Home", Paths.get(JkConstants.JEKA_BOOT_DIR).normalize() + " ( embedded !!! )");
        } else {
            txt.add("Jeka Home", JkLocator.getJekaHomeDir().normalize());
        }
        txt.add("Jeka User Home", JkLocator.getJekaUserHomeDir().toAbsolutePath().normalize());
        txt.add("Jeka Cache Dir",  JkLocator.getCacheDir().toAbsolutePath().normalize());
        JkProperties properties = JkRunbase.constructProperties(Paths.get(""));
        txt.add("Download Repos", JkRepoProperties.of(properties).getDownloadRepos().getRepos().stream()
                .map(JkRepo::getUrl).collect(Collectors.toList()));
        JkLog.info(txt.toString());
    }

    private static boolean embedded(Path jarFolder) {
        if (!Files.exists(bootDir())) {
            return false;
        }
        return JkUtilsPath.isSameFile(bootDir(), jarFolder);
    }

    private static Path bootDir() {
        return Paths.get(JkConstants.JEKA_BOOT_DIR);
    }

    // This class should lies outside PicocliMainCommand to be referenced inn annotation
    static class VersionProvider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() throws Exception {
            return new String[] {JkInfo.getJekaVersion()};
        }

    }

    private static void setupLogging(LogSettings logSettings, Path baseDir, String[] cmdLine) {
        JkLog.setDecorator(logSettings.style);

        if (logSettings.runtimeInformation) {
            displayRuntimeInfo(baseDir, cmdLine);
        }
        if (logSettings.debug) {
            JkLog.setVerbosity(JkLog.Verbosity.DEBUG);
        } else if(logSettings.verbose) {
            JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        }

        // By default, log working animation when working dir = base dir (this mean that we are not
        // invoking a tool packaged with JeKa.
        Path workingDir = Paths.get("");
        boolean logAnimation = baseDir.equals(workingDir);
        if (logSettings.animation != null) {
            logAnimation = logSettings.animation;
        }
        JkLog.setAcceptAnimation(logAnimation);
        JkLog.setShowTaskDuration(logSettings.duration);
    }

    private static void displayDuration(long startTs) {
        System.out.println("\nTotal run duration : " + JkUtilsTime.durationInSeconds(startTs) + " seconds.");
    }

    private static void logRuntimeInfoBase(Engine engine, JkProperties props) {
        JkLog.info(Jk2ColumnsText.of(18, 150)
                .add("Init KBean", engine.resolveKBeans().initKBeanClassname)
                .add("Default KBean", engine.resolveKBeans().defaultKbeanClassname)
                .toString());
        JkLog.info("Properties         :");
        JkLog.info(props.toColumnText(30, 90, !JkLog.isVerbose())
                .setMarginLeft("   | ")
                .setSeparator(" | ").toString());
        JkPathSequence cp = engine.getClasspathSetupResult().runClasspath;
        JkLog.info("Jeka Classpath     :");
        cp.forEach(entry -> JkLog.info("   | " + entry));
    }

    private static void logRuntimeInfoEngineCommands(KBeanAction.Container actionContainer) {
        JkLog.info("Commands           :");
        JkLog.info(actionContainer.toColumnText()
                .setSeparator(" | ")
                .setMarginLeft("   | ")
                .toString());
    }

    private static void logRuntimeInfoRun(JkRunbase runbase) {
        List<String> beanNames = runbase.getBeans().stream()
                .map(Object::getClass)
                .map(KBean::name)
                .collect(Collectors.toList());
        JkLog.info("Involved KBeans    :", beanNames);
        JkLog.info("    " + String.join(", ", beanNames));
        JkLog.info("");
    }

    private static void logOutro(LogSettings logs, long start) {
        if (logs.duration) {
            displayDuration(start);
        }
    }

    private static void handleGenericThrowable(Throwable t, long start, LogSettings logs) {
        JkBusyIndicator.stop();
        JkLog.restoreToInitialState();
        if (t.getMessage() != null) {
            System.err.println(t.getMessage());
        }
        System.err.println("You can investigate using --verbose, --debug, --stacktrace or -ls=DEBUG options.");
        System.err.println("If this originates from a bug, please report the issue at: " +
                "https://github.com/jeka-dev/jeka/issues");

        if ( (!(t instanceof JkException)) || shouldPrintExceptionDetails(logs)) {
            printException(logs, t);
        }
        System.err.println("Failed !");
    }

    private static boolean shouldPrintExceptionDetails(LogSettings logs) {
        return logs.debug || logs.stackTrace;
    }

    private static void printException(LogSettings logs, Throwable e) {
        System.err.println();
        if (logs.verbose || logs.stackTrace) {
            System.err.println("=============================== Stack Trace =============================================");
            e.printStackTrace(System.err);
            System.err.flush();
            System.err.println("=========================================================================================");
        }
    }

}
