package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.tool.CommandLine.Model.CommandSpec;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class PicocliMain {

    public static void main(String... args) {

        // Jeka needs to run in with a specific class loader
        if (!(Thread.currentThread().getContextClassLoader() instanceof AppendableUrlClassloader)) {
            final URLClassLoader urlClassLoader = new AppendableUrlClassloader();
            Thread.currentThread().setContextClassLoader(urlClassLoader);
            final Object[] argArray = new Object[]{args};
            JkClassLoader.of(urlClassLoader).invokeStaticMethod(false, PicocliMain.class.getName(),
                    "main", argArray);
            return;
        }

        // Remove -r arguments sent by shell script
        String[] filteredArgs = Main.filteredArgs(args);

        // Get the code base directory sent by script shell
        String basedirProp = System.getProperty("jeka.current.basedir");
        final Path baseDir = basedirProp == null ? Paths.get("")
                : Paths.get("").toAbsolutePath().normalize().relativize(Paths.get(basedirProp));

        // Handle --help
        // It needs to be fast and safe. Only loads KBeans found in current classpath
        if (isUsageHelpRequested(filteredArgs)) {

            CommandLine commandLine = PicocliCommands.mainCommandLine(baseDir);
            PicocliCommands.getStandardCommandSpecSafely(baseDir).forEach(commandSpec -> {
                String name = commandSpec.name() + ":";
                commandLine.addSubcommand(name, commandSpec);
            });
            commandLine.usage(commandLine.getOut());
            System.exit(commandLine.getCommandSpec().exitCodeOnUsageHelp());
        }

        // Handle --version
        if (isVersionHelpRequested(filteredArgs)) {
            CommandLine commandLine = PicocliCommands.stdHelp();
            commandLine.printVersionHelp(commandLine.getOut());
            System.exit(commandLine.getCommandSpec().exitCodeOnVersionHelp());
        }

        // Interpolate command line with values found in properties
        JkProperties props = JkRunbase.readProjectPropertiesRecursively(baseDir);
        String[] interpolatedArgs = Environment.interpolatedCommandLine(filteredArgs, props);

        // Get shared dependency resolver
        JkDependencyResolver dependencyResolver = dependencyResolver(baseDir);

        // TODO For now use the old parser -> migrate to picocli
        Environment.parsedCmdLine = ParsedCmdLine.parse(interpolatedArgs);
        Environment.parsedCmdLine.getSystemProperties().forEach(System::setProperty);
        Environment.originalArgs = filteredArgs;

        // Add classpath mentioned in command-line
        JkDependencySet cmdlineDeps = cmdlineDependencies(baseDir, interpolatedArgs);
        JkPathSequence extraClasspath = dependencyResolver.resolve(cmdlineDeps).getFiles();
        AppendableUrlClassloader.addEntriesOnContextClassLoader(extraClasspath);

        // Fill Environment.LogSettings & Environment.BehaviorSettings
        PicocliMainCommand mainCommand = new PicocliMainCommand(baseDir);
        CommandSpec mainCommandSpec = CommandSpec.forAnnotatedObject(mainCommand);
        CommandLine mainCommandLine = new CommandLine(mainCommandSpec);
        mainCommandLine.parseArgs(interpolatedArgs);
        Environment.logs = mainCommand.logSettings();
        Environment.behavior = mainCommand.behaviorSettings();

        // Resolve injected dependencies + compiled 'jeka-src'




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

    private static JkDependencySet cmdlineDependencies(Path baseDir, String[] cmdLineArgs) {
        CommandLine cpCommandline = PicocliCommands.classPathCommand();
        CommandLine.ParseResult cpParseResult = cpCommandline.parseArgs(cmdLineArgs);
        List<String> cmdlineCpArgs = cpParseResult.matchedOptionValue("-cp", Collections.emptyList());
        List<JkDependency> dependencies = cmdlineCpArgs.stream()
                .map(String::trim)
                .distinct()
                .map(desc -> ParsedCmdLine.toDependency(baseDir, desc))
                .collect(Collectors.toList());
        return JkDependencySet.of(dependencies);
    }

    private static JkDependencyResolver dependencyResolver(Path baseDir) {
        JkRepoSet repos = JkRepoProperties.of(JkRunbase.constructProperties(baseDir)).getDownloadRepos();
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of(repos);
        dependencyResolver.getDefaultParams().setFailOnDependencyResolutionError(true);
        return dependencyResolver;
    }
}
