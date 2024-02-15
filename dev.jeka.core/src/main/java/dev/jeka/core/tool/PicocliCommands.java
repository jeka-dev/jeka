package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.tool.CommandLine.Model.CommandSpec;
import dev.jeka.core.tool.CommandLine.Model.OptionSpec;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;
import dev.jeka.core.tool.builtins.tooling.git.GitKBean;
import dev.jeka.core.tool.builtins.tooling.ide.EclipseKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;
import dev.jeka.core.tool.builtins.tooling.nexus.NexusKBean;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class PicocliCommands {

    static CommandLine mainCommandLine(Path baseDir) {
        PicocliMainCommand mainCommand = new PicocliMainCommand(baseDir);
        CommandSpec mainCommandSpec = CommandSpec.forAnnotatedObject(mainCommand);
        return new CommandLine(mainCommandSpec);
    }

    static CommandLine stdHelp() {
        CommandSpec commandSpec = CommandSpec.create()
                .mixinStandardHelpOptions(true).helpCommand(true);
        return new CommandLine(commandSpec);
    }

    static CommandLine classPathCommand() {
        CommandSpec commandSpec = CommandSpec.create()
                .addOption(OptionSpec.builder("-cp", "--classpath")
                        .arity("0..*")
                        .build());
        CommandLine commandLine = new CommandLine(commandSpec);
        commandLine.setUnmatchedArgumentsAllowed(true);
        return commandLine;
    }

    static CommandSpec fromKBeanClass(Class<? extends KBean> kbeanClass) {
        KBeanDescription beanDesc = KBeanDescription.of(kbeanClass);
        CommandSpec spec = CommandSpec.create();
        spec.usageMessage()
                        .header(beanDesc.header)
                        .description(beanDesc.description.split("\n"));
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

    /*
     * Method for "--help" : should not fail, and be fast !!!
     * It has been measured at 100ms
     */
    static List<CommandSpec> getStandardCommandSpecSafely(Path baseDir) {
        try {
            List<Class<? extends KBean>> kbeanClasses = JkUtilsIterable.listOf(
                    BaseKBean.class,
                    ProjectKBean.class,
                    MavenKBean.class,
                    GitKBean.class,
                    DockerKBean.class,
                    IntellijKBean.class,
                    EclipseKBean.class,
                    NexusKBean.class
            );
            return kbeanClasses.stream()
                    .map(PicocliCommands::fromKBeanClass)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            JkLog.warn("Error while reading standard KBeans definitions : %s", e.getMessage());
            if (JkLog.isVerbose()) {
                e.printStackTrace();
            }
            return Collections.emptyList();
        }
    }

}
