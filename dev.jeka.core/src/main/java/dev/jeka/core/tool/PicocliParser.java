package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.CommandLine.Model.CommandSpec;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static dev.jeka.core.tool.EngineCommand.Action.*;

class PicocliParser {

    static final String KBEAN_CMD_SUFFIX = ":";

    /**
     * Parse both jeka.properties and command line to get KBean action.
     * The field injections declared after override ones declared before.
     * This means that command line overrides fields declared in jeka.properties
     */
    public static List<KBeanAction> parse(CmdLineArgs args, JkProperties props, EngineBase.KBeanResolution resolution) {
        KBeanActionContainer kBeanActionContainer = new KBeanActionContainer();
        kBeanActionContainer.addAll(parse(props, resolution));
        kBeanActionContainer.addAll(parse(args, resolution));
        return kBeanActionContainer.kBeanActions;
    }

    static List<KBeanAction> parse(CmdLineArgs args, EngineBase.KBeanResolution resolution) {
        return args.splitByKbeanContext().stream()
                .flatMap(scopedArgs -> createFromScopedArgs(scopedArgs, resolution, "").stream())
                .collect(Collectors.toList());
    }

    static List<KBeanAction> parse(JkProperties properties, EngineBase.KBeanResolution resolution) {
        return properties.getAllStartingWith("", true).entrySet().stream()
                .filter(entry -> KbeanAndField.isKBeanAndField(entry.getKey()))
                .map(entry -> KbeanAndField.of(entry.getKey(), entry.getValue()))
                .map(KbeanAndField::toCommandLineArgs)
                .flatMap(args -> createFromScopedArgs(args, resolution, JkConstants.PROPERTIES_FILE).stream())
                .collect(Collectors.toList());
    }

    private static List<KBeanAction> createFromScopedArgs(CmdLineArgs args, EngineBase.KBeanResolution resolution,
                                                          String source) {

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
            CommandLine cmdLine = allKBeanCommandLine(resolution.allKbeans, source);
            String msg = kbeanName == null ?
                    "No default KBean defined. You need to precise on which kbean apply '" + firstArg + "'"
                    : "No KBean names '" + kbeanName + "' found.";
            throw new CommandLine.ParameterException(cmdLine, msg);
        }
        Class<? extends KBean> kbeanClass = JkClassLoader.ofCurrent().load(kbeanClassName);

        // Add init action
        List<KBeanAction> kBeanActions = new LinkedList<>();
        kBeanActions.add(new KBeanAction(BEAN_INIT, kbeanClassName, null, null));

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
            KBeanAction kBeanAction = new KBeanAction(SET_FIELD_VALUE, kbeanName, name, value);
            kBeanActions.add(kBeanAction);
        }

        // Add Method invokes
        Arrays.stream(methodOrFieldArgs)
                .filter(availableMethodNames::contains)
                .forEach(name -> kBeanActions.add(new KBeanAction(INVOKE, kbeanName, name, null)));

        return kBeanActions;
    }

    /**
     * @param source comming from cmd line ("") or property file ("jeka.property')
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
            return new CmdLineArgs(kbean + KBEAN_CMD_SUFFIX, field + "=" + JkUtilsString.nullToEmpty(value));
        }

    }

    private static class KBeanActionContainer {

        private final List<KBeanAction> kBeanActions = new LinkedList<>();

        void addAll(List<KBeanAction> kBeanActions) {
            kBeanActions.forEach(this::add);
        }

        void add(KBeanAction kBeanAction) {

            // Always add invokes
            if (kBeanAction.action == INVOKE) {
                kBeanActions.add(kBeanAction);

            // Add instantiation only if it has not been already done
            } else if (kBeanAction.action == BEAN_INIT) {
                boolean present = kBeanActions.stream()
                        .filter(kBeanAction1 -> kBeanAction1.action == BEAN_INIT)
                        .anyMatch(kBeanAction1 -> kBeanAction.beanName.equals(kBeanAction1.beanName));
                if (!present) {
                    kBeanActions.add(kBeanAction);
                }

            // If field inject has already bean declared on same bean/field, it is replaced
            } else if (kBeanAction.action == SET_FIELD_VALUE) {
                KBeanAction present = kBeanActions.stream()
                        .filter(kBeanAction1 -> kBeanAction1.action == SET_FIELD_VALUE)
                        .filter(kBeanAction1 -> kBeanAction.beanName.equals(kBeanAction1.beanName))
                        .filter(kBeanAction1 -> kBeanAction.member.equals(kBeanAction1.member))
                        .findFirst().orElse(null);
                if (present == null) {
                    kBeanActions.add(kBeanAction);
                } else {
                    int index = kBeanActions.indexOf(present);
                    kBeanActions.remove(index);
                    kBeanActions.add(index, kBeanAction);
                }
            }
        }
    }
}
