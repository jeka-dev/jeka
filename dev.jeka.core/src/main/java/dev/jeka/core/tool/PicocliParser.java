package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static dev.jeka.core.tool.EngineCommand.Action.*;

class PicocliParser {

    static List<KBeanAction> parse(CmdLineArgs args, EngineBase.KBeanResolution resolution) {
        return args.splitByKbeanContext().stream()
                .flatMap(scopedArgs -> createFromScopedARgs(scopedArgs, resolution).stream())
                .collect(Collectors.toList());
    }

    static private List<KBeanAction> createFromScopedARgs(CmdLineArgs args, EngineBase.KBeanResolution resolution) {

        // Find KBean class tto which apply the args
        String firstArg = args.get()[0];
        final String kbeanName;
        final  String kbeanClassName;
        final String[] methodOrFieldArgs;
        if (CmdLineArgs.isKbeanRef(firstArg)) {
            kbeanName = firstArg.substring(0, firstArg.length() - 1);
            kbeanClassName = resolution.findKbeanClassName(kbeanName).orElse(null);
            methodOrFieldArgs = Arrays.copyOfRange(args.get(), 1, args.get().length);
        } else {
            kbeanName = null;
            kbeanClassName = resolution.defaultKbeanClassname;
            methodOrFieldArgs = args.get();
        }
        if (kbeanClassName == null) {
            CommandLine cmdLine = allKBeanCommandLine(resolution.allKbeans);
            String msg = kbeanName == null ?
                    "No default KBean defined. You need to precise on which kbean applu '" + firstArg + "'"
                    : "No KBean names '" + kbeanName + "' found.";
            throw new CommandLine.ParameterException(cmdLine, msg);
        }
        Class<? extends KBean> kbeanClass = JkClassLoader.ofCurrent().load(kbeanClassName);

        // Add init action
        List<KBeanAction> kBeanActions = new LinkedList<>();
        kBeanActions.add(new KBeanAction(BEAN_INSTANTIATION, kbeanClassName, null, null));

        // Add field-injection

        // --  Create a PicoCli commandLine to parse
        KBeanDescription kBeanDescription = KBeanDescription.of(kbeanClass, false);

        // -- Construct args to parse only fields
        List<String> availableMethodNames = kBeanDescription.beanMethods.stream()
                .map(beanMethod -> beanMethod.name)
                .collect(Collectors.toList());
        String[] fieldOnlyArgs = removeMethods(methodOrFieldArgs, availableMethodNames);

        // -- Do parse
        CommandLine commandLine = new CommandLine(PicocliCommands.fromKBeanDesc(kBeanDescription));
        CommandLine.ParseResult parseResult = commandLine.parseArgs(fieldOnlyArgs);
        for (CommandLine.Model.OptionSpec optionSpec : parseResult.matchedOptions()) {
            String name = optionSpec.names()[0];
            Object value = parseResult.matchedOptionValue(name, null);
            KBeanAction kBeanAction = new KBeanAction(PROPERTY_INJECT, kbeanName, name, value);
            kBeanActions.add(kBeanAction);
        }

        // Add Method invokes
        Arrays.stream(methodOrFieldArgs)
                .filter(availableMethodNames::contains)
                .forEach(name -> kBeanActions.add(new KBeanAction(METHOD_INVOKE, kbeanName, name, null)));

        return kBeanActions;
    }

    static CommandLine allKBeanCommandLine(List<String> kbeanClassNames) {
        CommandLine.Model.CommandSpec commandSpec = CommandLine.Model.CommandSpec.create();
        for (String kbeanClassName : kbeanClassNames) {
            String kbeanName  = KBean.name(kbeanClassName);
            commandSpec.addSubcommand(kbeanName, CommandLine.Model.CommandSpec.create());
        }
        return new CommandLine(commandSpec);
    }

    static String[] removeMethods(String[] args, List<String> methodNames) {
        return Arrays.stream(args)
                .filter(arg -> !methodNames.contains(arg))
                .toArray(String[]::new);
    }
}
