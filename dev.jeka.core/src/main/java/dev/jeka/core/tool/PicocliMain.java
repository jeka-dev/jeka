package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.tool.CommandLine.Model.CommandSpec;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

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

        // Interpolate command line with values found in properties
        JkProperties props = JkRunbase.readProjectPropertiesRecursively(baseDir);
        String[] augmentedArgs = Environment.interpolatedCommandLine(filteredArgs, props);

        // TODO For now use the old parser -> migrate to picocli
        Environment.parsedCmdLine = ParsedCmdLine.parse(augmentedArgs);
        Environment.parsedCmdLine.getSystemProperties().forEach(System::setProperty);
        Environment.originalArgs = filteredArgs;


        CommandLine stdHelpCmdLine = PicocliCommandSpecs.stdHelp();
        CommandLine.ParseResult stdHelpParseResult = stdHelpCmdLine.parseArgs(augmentedArgs);

        // Handle --help
        // It needs to be fast and safe. Only loads KBeans found in current classpath
        if (stdHelpParseResult.isUsageHelpRequested()) {

            PicocliMainCommand mainCommand = new PicocliMainCommand(baseDir);
            CommandSpec mainCommandSpec = CommandSpec.forAnnotatedObject(mainCommand);
            CommandLine commandLine = new CommandLine(mainCommandSpec);

            PicocliCommandSpecs.getStandardCommandSpecSafely(baseDir).forEach(commandSpec -> {
                String name = commandSpec.name() + ":";
                mainCommandSpec.addSubcommand(name, commandSpec);
            });
            commandLine.usage(commandLine.getOut());
            System.exit(commandLine.getCommandSpec().exitCodeOnUsageHelp());
        }

        // Handle --version
        if (stdHelpParseResult.isVersionHelpRequested()) {
            stdHelpCmdLine.printVersionHelp(stdHelpCmdLine.getOut());
            System.exit(stdHelpCmdLine.getCommandSpec().exitCodeOnVersionHelp());
        }

        // Handle all other cases : load KBeans classes present in
        Engine engine = new Engine(baseDir);

    }

    // This class should lies outside PicocliMainCommand to be referenced inn annotation
    static class VersionProvider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() throws Exception {
            return new String[] {JkInfo.getJekaVersion()};
        }

    }
}
