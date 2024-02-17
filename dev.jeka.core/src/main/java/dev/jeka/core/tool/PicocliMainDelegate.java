package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkConsoleSpinner;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.text.Jk2ColumnsText;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsTime;
import dev.jeka.core.tool.CommandLine.Model.CommandSpec;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

class PicocliMainDelegate {

    // Method called by reflection in Main
    public static void doMain(String[] args) {

        // Remove -r arguments sent by shell script
        String[] filteredArgs = Main.filteredArgs(args);

        // Get the code base directory sent by script shell
        String basedirProp = System.getProperty("jeka.current.basedir");
        final Path baseDir = basedirProp == null ? Paths.get("")
                : Paths.get("").toAbsolutePath().normalize().relativize(Paths.get(basedirProp));

        // Handle --help
        // It needs to be fast and safe. Only loads KBeans found in current classpath
        if (isUsageHelpRequested(filteredArgs)) {
            PicoCliHelp.printUsageHelp(System.out);
            System.exit(0);
        }

        // Handle --version
        if (isVersionHelpRequested(filteredArgs)) {
            PicoCliHelp.printVersionHelp(System.out);
            System.exit(0);
        }

        // Interpolate command line with values found in properties
        JkProperties props = JkRunbase.constructProperties(baseDir);
        String[] interpolatedArgs = Environment.interpolatedCommandLine(filteredArgs, props);

        // first, parse only options
        PicocliMainCommand mainCommand = new PicocliMainCommand();
        CommandLine commandLine = new CommandLine(CommandSpec.forAnnotatedObject(mainCommand));
        String[] options = Arrays.stream(interpolatedArgs)
                .filter(arg -> arg.startsWith("-"))
                .toArray(String[]::new);
        commandLine.parseArgs(options);
        EnvLogSettings logs = mainCommand.logSettings();
        EnvBehaviorSettings behavior = mainCommand.behaviorSettings();
        JkDependencySet dependencies = mainCommand.dependencies();

        // setup log engine
        setLogs(logs, baseDir);

        // Compile and resolve deps for jeka-src
        JkRepoSet downloadRepos = JkRepoProperties.of(props).getDownloadRepos();
        EngineBase engineBase = EngineBase.of(baseDir, downloadRepos, dependencies, logs, behavior);
        JkConsoleSpinner.of("Booting JeKa...").run(engineBase::resolveKBeans);
        if (logs.runtimeInformation) {
            logRuntimeInfoChapter1(engineBase, props);
        }

        // Handle context help
        if (behavior.commandHelp.isPresent()) {
            EngineBase.KBeanResolution kBeanResolution = engineBase.resolveKBeans();
            String kbeanName = behavior.commandHelp.get();
            if (JkUtilsString.isBlank(kbeanName)) {
                PicoCliHelp.printCommandHelp(
                                engineBase.resolveClassPaths().runClasspath,
                                kBeanResolution.allKbeans,
                                kBeanResolution.defaultKbeanClassname, props, System.out);
                System.exit(0);
            }
            boolean found = PicoCliHelp.printKBeanHelp(
                    engineBase.resolveClassPaths().runClasspath,
                    kBeanResolution.allKbeans,
                    kbeanName, System.out);
            System.exit(found ? 0 : 1);
        }
    }

    // This class should lies outside PicocliMainCommand to be referenced inn annotation
    static class VersionProvider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() throws Exception {
            return new String[] {JkInfo.getJekaVersion()};
        }

    }

    private static boolean isUsageHelpRequested(String[] args) {
        return args.length == 0 || args[0].equals("--help") || args[0].equals("-h");
    }

    private static boolean isVersionHelpRequested(String[] args) {
        return args.length > 0 && (args[0].equals("--version") || args[0].equals("-V"));
    }

    private static JkDependencyResolver dependencyResolver(Path baseDir) {
        JkRepoSet repos = JkRepoProperties.of(JkRunbase.constructProperties(baseDir)).getDownloadRepos();
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of(repos);
        dependencyResolver.getDefaultParams().setFailOnDependencyResolutionError(true);
        return dependencyResolver;
    }

    private static void setLogs(EnvLogSettings logSettings, Path baseDir) {
        JkLog.setDecorator(logSettings.style);
        if (logSettings.banner) {
            displayIntro();
        }
        if (logSettings.runtimeInformation) {
            JkInit.displayRuntimeInfo(baseDir);
        }

        // By default, log working animation when working dir = base dir (this mean that we are not
        // invoking a tool packaged with JeKa.
        Path workingDir = Paths.get("");
        boolean logAnimation = baseDir.equals(workingDir);
        if (logSettings.animation != null) {
            logAnimation = logSettings.animation;
        }
        JkLog.setAcceptAnimation(logAnimation);
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

    private static void logRuntimeInfoChapter1(EngineBase engineBase, JkProperties props) {
        JkLog.info(Jk2ColumnsText.of(18, 150)
                .add("Init KBean", engineBase.resolveKBeans().initKBeanClassname)
                .add("Default KBean", engineBase.resolveKBeans().defaultKbeanClassname)
                .toString());
        JkLog.info("Properties         :");
        JkLog.info(props.toColumnText(30, 90, !JkLog.isVerbose())
                .setMarginLeft("   | ")
                .setSeparator(" | ").toString());
        JkPathSequence cp = engineBase.getClasspathSetupResult().runClasspath;
        JkLog.info("Jeka Classpath     :");
        cp.forEach(entry -> JkLog.info("   | " + entry));
    }

}
