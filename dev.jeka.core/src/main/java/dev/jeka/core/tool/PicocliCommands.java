package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.CommandLine.Help.Ansi.Style;
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

import java.util.*;
import java.util.stream.Collectors;

class PicocliCommands {

    static final List<Class<? extends KBean>> STANDARD_KBEAN_CLASSES = JkUtilsIterable.listOf(
            BaseKBean.class,
            ProjectKBean.class,
            MavenKBean.class,
            GitKBean.class,
            DockerKBean.class,
            IntellijKBean.class,
            EclipseKBean.class,
            NexusKBean.class
    );

    static CommandLine mainCommandLine() {
        PicocliMainCommand mainCommand = new PicocliMainCommand();
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
        return fromKBeanDesc(beanDesc);
    }

    static CommandSpec fromKBeanDesc(KBeanDescription beanDesc) {
        CommandSpec spec = CommandSpec.create();
        beanDesc.beanFields.forEach(beanField -> {
            String defaultValue = beanField.defaultValue == null  ? CommandLine.Option.NULL_VALUE
                    : Objects.toString(beanField.defaultValue);
            String description = beanField.description == null ?
                    "No description" : beanField.description;
            OptionSpec optionSpec = OptionSpec.builder(beanField.name)
                    .description(description)
                    .type(beanField.type)
                    .showDefaultValue(CommandLine.Help.Visibility.ALWAYS)
                    .hideParamSyntax(false)
                    .defaultValue(defaultValue)
                    .build();
            spec.addOption(optionSpec);
        });
        beanDesc.beanMethods.forEach(beanMethod -> {
            spec.addSubcommand(beanMethod.name, fromKBeanMethod(beanMethod));
        });
        return spec;
    }

    private static CommandSpec fromKBeanMethod(KBeanDescription.BeanMethod beanMethod) {
        CommandSpec spec = CommandSpec.create();
        spec.subcommandsRepeatable(true);
        spec.usageMessage().header(beanMethod.description);
        return spec;
    }

    /*
     * Method for "--help" : should not fail, and be fast !!!
     * It has been measured at 100ms
     */
    static List<CommandSpec> getStandardCommandSpecSafely() {
        try {
            return STANDARD_KBEAN_CLASSES.stream()
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

    static CommandSpec helpEntryPointCommand(ClassLoader classLoader,
                                             List<String> kbeanClassNames,
                                             String defaultKBeanClassName) {

        // Add commands and options from defaultKBean

        final CommandSpec main;
        KBeanDescription beanDescription = null;
        if (defaultKBeanClassName != null) {
            Class<? extends KBean> defaultKBeanClass = JkClassLoader.of(classLoader).load(defaultKBeanClassName);
            beanDescription = KBeanDescription.of(defaultKBeanClass);
            main = fromKBeanDesc(beanDescription);
        } else {
            main = CommandSpec.create().name("anonymous");
        }

        // Add sub-command for each kbean
        List<String> others = new LinkedList<>(kbeanClassNames);
        others.remove(defaultKBeanClassName);
        Map<String, Class<? extends KBean>> kbeanNameClassMap = beanNameClassMap(classLoader, others);
        for (Map.Entry<String, Class<? extends KBean>> entry : kbeanNameClassMap.entrySet()) {
            CommandSpec subSpec = fromKBeanClass(entry.getValue());
            main.addSubcommand(entry.getKey(), subSpec);
        }

        // Configure Usage
        main.usageMessage()
                .synopsisHeading("");
        List<String> synopsis = new LinkedList<>();
        if (defaultKBeanClassName != null) {
            synopsis.add("Default KBean: " + defaultKBeanClassName);
            if (!JkUtilsString.isBlank(beanDescription.header)) {
                synopsis.add("Description  : " + beanDescription.header);
            }
            synopsis.add("Options      :");
        }


        main.usageMessage()
                .customSynopsis(synopsis.toArray(new String[0]))
                .commandListHeading("Available KBeans:\n");

        return main;
    }



    private static Map<String, Class<? extends KBean>> beanNameClassMap(ClassLoader classLoader,
                                                                     List<String> kbeanClasses) {
        Map<String, Class<? extends KBean>> result = new HashMap<>();
        kbeanClasses.stream().forEach(className -> {
            Class<? extends KBean> clazz = JkClassLoader.of(classLoader).load(className);
            result.put(KBean.name(className), clazz);
        });
        return result;
    }



}
