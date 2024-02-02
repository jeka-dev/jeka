package dev.jeka.core.tool;

import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.CommandLine.Model.CommandSpec;
import dev.jeka.core.tool.CommandLine.Model.OptionSpec;
import dev.jeka.core.tool.CommandLine.ParseResult;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

class PicocliCommands {

    static CommandSpec fromKBeanClass(Class<? extends KBean> kbeanClass) {
        KBeanDescription beanDesc = KBeanDescription.renderItem(kbeanClass);
        CommandSpec spec = CommandSpec.create();
        spec.name(KBean.name(kbeanClass));
        spec.mixinStandardHelpOptions(true);
        beanDesc.beanFields.forEach(beanField -> {
            OptionSpec optionSpec = OptionSpec.builder(beanField.name)
                    .description(beanField.description)
                    .type(beanField.type)
                    .defaultValue(Objects.toString(beanField.defaultValue))
                    .build();
            spec.addOption(optionSpec);
        });
        return spec;
    }

    static CommandLine createCommandLine() {
        CommandSpec spec = CommandSpec.create();
        spec.mixinStandardHelpOptions(true); // usageHelp and versionHelp options
        spec.name("jeka");
        // spec.version(JkInfo.getJekaVersion());  /// don't see result i help

        populateOptions(spec);
       // spec.addOption(OptionSpec.builder("-v", "--verbose")

       //         .description("Log verbose messages.").build());

/*
        spec.addOption(OptionSpec.builder("-c", "--count")
                .paramLabel("COUNT")
                .type(int.class)
                .description("number of times to execute").build());
        spec.addPositional(PositionalParamSpec.builder()
                .paramLabel("FILES")
                .type(List.class).auxiliaryTypes(File.class) // List<File>
                .description("The files to process").build());
                *
 */
        return new CommandLine(spec);
    }



    static int run(ParseResult pr) {
        // handle requests for help or version information
        Integer helpExitCode = CommandLine.executeHelpRequest(pr);
        if (helpExitCode != null) { return helpExitCode; }

        // implement the business logic
        int count = pr.matchedOptionValue('c', 1);
        List<File> files = pr.matchedPositionalValue(0, Collections.<File>emptyList());
        for (File f : files) {
            for (int i = 0; i < count; i++) {
                System.out.println(i + " " + f.getName());
            }
        }
        return files.size();
    }

    public static void main(String[] args) {
        CommandLine commandLine = createCommandLine();

        // set an execution strategy (the run(ParseResult) method) that will be called
        // by CommandLine.execute(args) when user input was valid
        commandLine.setExecutionStrategy(PicocliCommands::run);
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    private static void populateOptions(CommandSpec spec) {
        spec.addOption(opt("-it", "--ivy-traces", "Log Ivy traces."));
        spec.addOption(opt("-v", "--verbose", "Log verbose messages."));

        spec.addOption(opt("-ri", "--runtime-info", "Log current JeKa environment info."));
    }

    private static OptionSpec opt(String shortOpt, String longOpt, String description) {
        return OptionSpec.builder(shortOpt, longOpt)
                .description(description)
                .build();
    }
}
