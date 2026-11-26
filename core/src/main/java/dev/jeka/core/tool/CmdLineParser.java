/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.CommandLine.Model.CommandSpec;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

class CmdLineParser {

    /**
     * Parse both jeka.properties and command line to get KBean action.
     * The field injections declared after override ones declared before.
     * This means that command line overrides fields declared in jeka.properties
     */
    public static KBeanAction.Container parse(CmdLineArgs args, KBeanResolution resolution) {
        KBeanAction.Container container = new KBeanAction.Container();
        container.addAll(parseCmdLineArgs(args, resolution));
        return container;
    }

    private static List<KBeanAction> parseCmdLineArgs(CmdLineArgs args, KBeanResolution resolution) {
        return args.splitByKbeanContext().stream()
                .flatMap(scopedArgs -> createFromScopedArgs(scopedArgs, resolution, "cmd line").stream())
                .collect(Collectors.toList());
    }

    /*
     * Scoped args contains only arguments scoped to a unique KBean
     */
    private static List<KBeanAction> createFromScopedArgs(CmdLineArgs args, KBeanResolution resolution,
                                                          String source) {

        if (args.isEmpty()) {
            return Collections.emptyList();
        }
        String kbeanName = args.findKbeanName();
        String kbeanClassName = resolution.findKbeanClassName(kbeanName).orElse(resolution.defaultKbeanClassName);
        final String[] methodOrFieldArgs = args.truncatedKBeanRef().get();

        if (kbeanClassName == null) {
            CommandLine cmdLine = allKBeanCommandLine(resolution.allKbeanClassNames, source);
            String origin = source.isEmpty() ? "." : " (from " + source + ").";
            String firstArg = args.isEmpty() ? "" : args.get()[0];
            String msg = JkUtilsString.isBlank(kbeanName)  ?
                    "No default KBean defined. You need to precise on which kbean apply '" + firstArg + "'"
                    : "No KBean found for name '" + kbeanName + "'";
            if (JkLog.isVerbose()) {
                msg = msg + ". Available KBeans : \n    " + String.join("\n    ", resolution.allKbeanClassNames);
            }
            throw new CommandLine.ParameterException(cmdLine, msg + origin);
        }
        Class<? extends KBean> kbeanClass = JkClassLoader.ofCurrent().load(kbeanClassName);

        // Add init action
        List<KBeanAction> kBeanActions = new LinkedList<>();
        kBeanActions.add(KBeanAction.ofInitialization(kbeanClass));

        // Add field-injection

        // -- Create a PicoCli commandLine to parse
        JkBeanDescription kBeanDescription = JkBeanDescription.of(kbeanClass, true);

        // -- Construct args to parse only fields
        List<String> availableMethodNames = kBeanDescription.beanMethods.stream()
                .map(beanMethod -> beanMethod.name)
                .collect(Collectors.toList());
        String[] fieldOnlyArgs = removeMethods(methodOrFieldArgs, availableMethodNames);

        // -- Do parse
        CommandLine commandLine = new CommandLine(PicocliCommands.fromKBeanDesc(kBeanDescription,
                Arrays.asList(fieldOnlyArgs)));
        // -- Field values may be equal to a method name (e.g. buildDir=build), or to a field name (e.g. version=moduleId)
        commandLine.setAllowSubcommandsAsOptionParameters(true);
        commandLine.setAllowOptionsAsOptionParameters(true);
        checkNoFieldTakenAsValue(commandLine, fieldOnlyArgs);
        CommandLine.ParseResult parseResult = commandLine.parseArgs(withExplicitBooleans(commandLine, fieldOnlyArgs));
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

    /*
     * As field names are accepted as values, 'version moduleId=x' would set 'version' to 'moduleId=x'.
     * A field expecting a value, followed by another field, fails instead, as it did before.
     */
    private static void checkNoFieldTakenAsValue(CommandLine commandLine, String[] args) {
        CommandLine.Model.CommandSpec spec = commandLine.getCommandSpec();
        for (int i = 0; i < args.length - 1; i++) {
            CommandLine.Model.OptionSpec option = spec.findOption(args[i]);
            if (option == null || option.arity().min() == 0) {
                continue;
            }
            String nextName = args[i + 1].split("=", 2)[0];
            if (spec.findOption(nextName) != null) {
                throw new CommandLine.ParameterException(commandLine, String.format(
                        "Expected parameter for option '%s' but found '%s'", args[i], args[i + 1]));
            }
        }
    }

    /*
     * A boolean field without value (e.g. 'test.skip') means true. Picocli would flip its default value instead,
     * which fails when the default value is unknown.
     */
    private static String[] withExplicitBooleans(CommandLine commandLine, String[] args) {
        CommandLine.Model.CommandSpec spec = commandLine.getCommandSpec();
        return Arrays.stream(args)
                .map(arg -> {
                    CommandLine.Model.OptionSpec option = spec.findOption(arg);
                    boolean isBoolean = option != null
                            && (option.type() == boolean.class || option.type() == Boolean.class);
                    return isBoolean ? arg + "=true" : arg;
                })
                .toArray(String[]::new);
    }

    private static String[] removeMethods(String[] args, List<String> methodNames) {
        return Arrays.stream(args)
                .filter(arg -> !methodNames.contains(arg))
                .toArray(String[]::new);
    }

}
