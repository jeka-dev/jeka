package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.CommandLine.Model.CommandSpec;

import java.util.*;
import java.util.stream.Collectors;

class PicocliParser {

    private static final String ENV_SYS_PROP_SOURCE = "env-sysProps";

    /**
     * Parse both jeka.properties and command line to get KBean action.
     * The field injections declared after override ones declared before.
     * This means that command line overrides fields declared in jeka.properties
     */
    public static KBeanAction.Container parse(CmdLineArgs args, JkProperties props, Engine.KBeanResolution resolution) {
        KBeanAction.Container container = new KBeanAction.Container();
        container.addAll(parseSysProps(args, resolution));
        container.addAll(parsePropertyFile(props, resolution));
        container.addAll(parseCmdLineArgs(args, resolution));
        return container;
    }

    private static List<KBeanAction> parseCmdLineArgs(CmdLineArgs args, Engine.KBeanResolution resolution) {
        return args.splitByKbeanContext().stream()
                .flatMap(scopedArgs -> createFromScopedArgs(scopedArgs, resolution, "cmd line").stream())
                .collect(Collectors.toList());
    }

    private static List<KBeanAction> parsePropertyFile(JkProperties properties, Engine.KBeanResolution resolution) {
        return properties.getAllStartingWith("", true).entrySet().stream()
                .filter(entry -> KbeanAndField.isKBeanAndField(entry.getKey()))
                .map(entry -> KbeanAndField.of(entry.getKey(), entry.getValue()))
                .map(KbeanAndField::toCommandLineArgs)
                .flatMap(args -> createFromScopedArgs(args, resolution, JkConstants.PROPERTIES_FILE).stream())
                .collect(Collectors.toList());
    }

    private static List<KBeanAction> parseSysProps(CmdLineArgs args, Engine.KBeanResolution resolution) {
        List<String> involvedKBeanClasses = args.splitByKbeanContext().stream()
                .map(CmdLineArgs::findKbeanName)
                .map(resolution::findKbeanClassName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .collect(Collectors.toList());

        List<KBeanAction> actions = new LinkedList<>();
        for (String kbeanClassName : involvedKBeanClasses) {
            Class<? extends  KBean> kbeanClass = JkClassLoader.ofCurrent().load(kbeanClassName);
            KBeanDescription desc = KBeanDescription.of(kbeanClass, true);
            CommandLine commandLine = new CommandLine(PicocliCommands.fromKBeanDesc(desc));
            commandLine.parseArgs();
            CommandSpec commandSpec = commandLine.getCommandSpec();
            for (KBeanDescription.BeanField beanField : desc.beanFields) {
                if (beanField.injectedPropertyName == null) {
                    continue;
                }
                Object value = commandSpec.findOption(beanField.name).getValue();

                // To avoid 'null' field-set, skip when sys prop = default value
                if (!Objects.equals(value, beanField.defaultValue)) {
                    actions.add(KBeanAction.ofSetValue(kbeanClass, beanField.name, value, ENV_SYS_PROP_SOURCE));
                }
            }
        }
        return actions;
    }

    /*
     * Scoped args contains only arguments scoped to a unique KBean
     */
    private static List<KBeanAction> createFromScopedArgs(CmdLineArgs args, Engine.KBeanResolution resolution,
                                                          String source) {

        String kbeanName = args.findKbeanName();
        String kbeanClassName = resolution.findKbeanClassName(kbeanName).orElse(null);
        final String[] methodOrFieldArgs = args.trunkKBeanRef().get();

        if (kbeanClassName == null) {
            CommandLine cmdLine = allKBeanCommandLine(resolution.allKbeans, source);
            String origin = source.isEmpty() ? "." : " (from " + source + ").";
            String firstArg = args.isEmpty() ? "" : args.get()[0];
            String msg = JkUtilsString.isBlank(kbeanName)  ?
                    "No default KBean defined. You need to precise on which kbean apply '" + firstArg + "'"
                    : "No KBean found for name '" + kbeanName + "'";
            if (JkLog.isVerbose()) {
                msg = msg + ". Available KBeans : \n    " + String.join("\n    ", resolution.allKbeans);
            }
            throw new CommandLine.ParameterException(cmdLine, msg + origin);
        }
        Class<? extends KBean> kbeanClass = JkClassLoader.ofCurrent().load(kbeanClassName);

        // Add init action
        List<KBeanAction> kBeanActions = new LinkedList<>();
        kBeanActions.add(KBeanAction.ofInit(kbeanClass));

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
            KBeanAction kBeanAction = KBeanAction.ofSetValue(kbeanClass, name, value, source);
            kBeanActions.add(kBeanAction);
        }

        // Add Method invokes
        Arrays.stream(methodOrFieldArgs)
                .filter(availableMethodNames::contains)
                .forEach(name -> kBeanActions.add(KBeanAction.ofInvoke(kbeanClass, name)));
        return kBeanActions;
    }

    /**
     * @param source coming from cmd line ("") or property file ("jeka.property')
     */
    private static CommandLine allKBeanCommandLine(List<String> kbeanClassNames, String source) {
        CommandSpec commandSpec = CommandSpec.create();
        for (String kbeanClassName : kbeanClassNames) {
            String kbeanName  = KBean.name(kbeanClassName);
            commandSpec.addSubcommand(kbeanName, CommandSpec.create());
        }
        return new CommandLine(commandSpec.name(source));
    }

    private static String[] removeMethods(String[] args, List<String> methodNames) {
        return Arrays.stream(args)
                .filter(arg -> !methodNames.contains(arg))
                .toArray(String[]::new);
    }

    private static class KbeanAndField {

        final String kbean;

        final String field;

        final String value;

        KbeanAndField(String kbean, String field, String value) {
            this.kbean = kbean;
            this.field = field;
            this.value = value;
        }

        static boolean isKBeanAndField(String candidateKey) {
            return candidateKey.contains("#");
        }

        static KbeanAndField of(String key, String value) {
            String kbean = JkUtilsString.substringBeforeFirst(key, "#");
            String field = JkUtilsString.substringAfterFirst(key, "#");
            return new KbeanAndField(kbean, field, value);
        }

        CmdLineArgs toCommandLineArgs() {
            return new CmdLineArgs(kbean + JkConstants.KBEAN_CMD_SUFFIX, field + "=" + JkUtilsString.nullToEmpty(value));
        }

    }

}
