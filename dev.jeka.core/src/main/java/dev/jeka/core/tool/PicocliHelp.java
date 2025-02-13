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

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

class PicocliHelp {

    private static final String SECTION_OTHER_KBEANS_HEADING = "otherKBeanHeading";

    private static final String SECTION_OTHER_KBEANS_DETAILS = "otherKbeanDetails";

    private static final String SECTION_STD_KBEANS_HEADING = "stdKBeanHeading";

    private static final String SECTION_STD_KBEANS_DETAILS = "stdKbeanDetails";

    private static final String SECTION_SHORTHANDS_HEADING = "shorthandHeading";

    private static final String SECTION_SHORTHANDS_DETAILS = "shortHandDetails";

    static void printUsageHelp(PrintStream printStream) {
        CommandLine commandLine = PicocliCommands.mainCommandLine();
        commandLine.usage(printStream, colorScheme());
    }

    static void printVersionHelp(PrintStream printStream) {
        printStream.print(JkInfo.getJekaVersion());  // no need of carriage return
    }

    static void printCmdHelp(JkPathSequence classpath,
                             KBeanResolution kbeanResolution,
                             JkProperties props,
                             PrintStream printStream) {
        cmdHelp(classpath, kbeanResolution, props)
                .usage(printStream, colorScheme());
    }

    // return false if kbeanName does not math any KBean
    static boolean printKBeanHelp(JkPathSequence classpath,
                                 List<String> kbeanClassNames,
                                 String kbeanName,
                                 JkRunbase runbase,
                                 PrintStream printStream) {
        CommandLine commandLine = kbeanHelp(classpath, kbeanClassNames, kbeanName, runbase);
        if (commandLine == null) {
            return false;
        }
        commandLine.usage(printStream, colorScheme());
        return true;
    }

    private static CommandLine cmdHelp(
            JkPathSequence classpath,
            KBeanResolution kbeanResolution,
            JkProperties props) {

        String defaultKBeanClassName = kbeanResolution.defaultKbeanClassName;

        ClassLoader classLoader = JkUrlClassLoader.of(classpath).get();

        // Add commands and options from default KBean
        final CommandLine.Model.CommandSpec main;
        JkBeanDescription beanDescription = null;
        if (defaultKBeanClassName != null) {
            Class<? extends KBean> defaultKBeanClass = JkClassLoader.of(classLoader).load(defaultKBeanClassName);
            beanDescription = JkBeanDescription.of(defaultKBeanClass);
            main = PicocliCommands.fromKBeanDesc(beanDescription);
        } else {
            main = dev.jeka.core.tool.CommandLine.Model.CommandSpec.create().name("");
        }

        // Configure Usage
        main.usageMessage().synopsisHeading("");
        List<String> synopsis = new LinkedList<>();

        // -- Case defaultKBean is present
        if (defaultKBeanClassName != null) {
            synopsis.add(String.format("Default KBean @|yellow %s:|@ (%s)",
                    KBean.name(defaultKBeanClassName), defaultKBeanClassName));
            if (!JkUtilsString.isBlank(beanDescription.synopsisHeader)) {
                synopsis.add("Description  : " + beanDescription.synopsisHeader);
            }
            synopsis.add("\nFields");
        }
        main.usageMessage()
                .autoWidth(true)
                .customSynopsis(synopsis.toArray(new String[0]))
                .commandListHeading("Methods\n");

        // Add section for Standard KBeans
        Map<String, String> stdKBeans = new LinkedHashMap<>();
        for (Class<? extends KBean> kbeanClass : JkBeanDescription.STANDARD_KBEAN_CLASSES) {
            JkBeanDescription description = JkBeanDescription.of(kbeanClass);
            String name = KBean.name(kbeanClass);
            stdKBeans.put( String.format("@|yellow %s:|@", name), description.synopsisHeader);
        }
        CommandLine commandLine = new CommandLine(main);
        commandLine.getHelpSectionMap().put(SECTION_STD_KBEANS_HEADING,
                help -> help.createHeading("\nStandard KBeans\n"));
        commandLine.getHelpSectionMap().put(SECTION_STD_KBEANS_DETAILS,
                help -> help.createTextTable(stdKBeans).toString());
        List<String> keys = new ArrayList<>(commandLine.getHelpSectionKeys());
        int index = keys.indexOf(dev.jeka.core.tool.CommandLine.Model.UsageMessageSpec.SECTION_KEY_FOOTER_HEADING);
        keys.add(index, SECTION_STD_KBEANS_HEADING);
        keys.add(index + 1, SECTION_STD_KBEANS_DETAILS);
        commandLine.setHelpSectionKeys(keys);

        // Add section for other KBeans
        List<String> others = new LinkedList<>(kbeanResolution.allKbeanClassNames);
        List<String> stdKbeanClassNames = JkBeanDescription.STANDARD_KBEAN_CLASSES.stream()
                        .map(Class::getName).collect(Collectors.toList());
        others.removeAll(stdKbeanClassNames);
        others.remove(defaultKBeanClassName);
        if (!others.isEmpty()) {
            Map<String, Class<? extends KBean>> kbeanNameClassMap = beanNameClassMap(classLoader, others);
            Map<String, String> kbeans = new HashMap<>();
            for (Map.Entry<String, Class<? extends KBean>> entry : kbeanNameClassMap.entrySet()) {
                JkBeanDescription description = JkBeanDescription.of(entry.getValue());
                kbeans.put(String.format("@|yellow %s:|@", entry.getKey()), description.synopsisHeader);
            }
            commandLine.getHelpSectionMap().put(SECTION_OTHER_KBEANS_HEADING,
                    help -> help.createHeading("\nOther KBeans\n"));
            commandLine.getHelpSectionMap().put(SECTION_OTHER_KBEANS_DETAILS,
                    help -> help.createTextTable(kbeans).toString());
            index = keys.indexOf(dev.jeka.core.tool.CommandLine.Model.UsageMessageSpec.SECTION_KEY_FOOTER_HEADING);
            keys.add(index, SECTION_OTHER_KBEANS_HEADING);
            keys.add(index + 1, SECTION_OTHER_KBEANS_DETAILS);
            commandLine.setHelpSectionKeys(keys);
        }

        // Add Section for shortcut
        Map<String, String> shorthands = cmdShortHand(props);
        if (!shorthands.isEmpty()) {
            commandLine.getHelpSectionMap().put(SECTION_SHORTHANDS_HEADING,
                    help -> help.createHeading("\nShorthands\n"));
            commandLine.getHelpSectionMap().put(SECTION_SHORTHANDS_DETAILS,
                    help -> help.createTextTable(shorthands).toString());
            index = keys.indexOf(dev.jeka.core.tool.CommandLine.Model.UsageMessageSpec.SECTION_KEY_FOOTER_HEADING);
            keys.add(index, SECTION_SHORTHANDS_HEADING);
            keys.add(index + 1, SECTION_SHORTHANDS_DETAILS);
            commandLine.setHelpSectionKeys(keys);
        }

        main.usageMessage()
                .footer("", "Execute @|yellow jeka <kbean>: --doc|@ (as @|italic jeka docker: --doc|@) " +
                        "to get details on specific KBean.");

        return commandLine;

    }

    private static CommandLine kbeanHelp(
            JkPathSequence classpath,
            List<String> kbeanClassNames,
            String kbeanName,
            JkRunbase runbase) {

        String kbeanClassName = kbeanClassNames.stream()
                .filter(clazzName -> KBean.nameMatches(clazzName, kbeanName))
                .findFirst().orElse(null);
        if (kbeanClassName == null) {
            return null;
        }

        ClassLoader classLoader = JkUrlClassLoader.of(classpath).get();
        Class<? extends KBean> defaultKBeanClass = JkClassLoader.of(classLoader).load(kbeanClassName);
        JkBeanDescription beanDescription = JkBeanDescription.ofWithDefaultValues(defaultKBeanClass, runbase);
        CommandLine.Model.CommandSpec main = PicocliCommands.fromKBeanDesc(beanDescription);

        // Configure Usage
        main.usageMessage().synopsisHeading("");
        List<String> synopsis = new LinkedList<>();
        synopsis.add(String.format("KBean @|yellow %s:|@ (%s)",
                    KBean.name(kbeanName), kbeanClassName));
        synopsis.add("");
        if (!JkUtilsString.isBlank(beanDescription.synopsisHeader)) {
            String header = beanDescription.synopsisHeader.trim();
            header = header.endsWith(".") ? header : header + ".";
            synopsis.add(header);
            List<String> descLines = Arrays.asList(beanDescription.synopsisDetail.split("\n"));
            synopsis.addAll(descLines);
        }
        if (!beanDescription.beanFields.isEmpty()) {
            synopsis.add("Fields");
        }
        main.usageMessage()
                .autoWidth(true)
                .customSynopsis(synopsis.toArray(new String[0]))
                .commandListHeading("Methods\n");

        return new CommandLine(main).setUsageHelpAutoWidth(true);
    }


    private static Map<String, Class<? extends KBean>> beanNameClassMap(ClassLoader classLoader,
                                                                        List<String> kbeanClasses) {
        Map<String, Class<? extends KBean>> result = new LinkedHashMap<>();
        kbeanClasses.stream().forEach(className -> {

            // A class being in the .jeka-work cache might no longer exist.
            try {
                Class<? extends KBean>clazz = JkClassLoader.of(classLoader).load(className);
                result.put(KBean.name(className), clazz);
            } catch (Exception e) {
                JkLog.warn("Class " + className + " could not be loaded.");
            }
        });
        return result;
    }

    private static CommandLine.Help.ColorScheme colorScheme() {
        return new CommandLine.Help.ColorScheme.Builder()
                .commands    (dev.jeka.core.tool.CommandLine.Help.Ansi.Style.fg_yellow)    // combine multiple styles
                .options     (dev.jeka.core.tool.CommandLine.Help.Ansi.Style.fg_yellow)                // yellow foreground color
                .parameters  (dev.jeka.core.tool.CommandLine.Help.Ansi.Style.fg_yellow)
                .optionParams(dev.jeka.core.tool.CommandLine.Help.Ansi.Style.italic)
                .errors      (dev.jeka.core.tool.CommandLine.Help.Ansi.Style.fg_red, dev.jeka.core.tool.CommandLine.Help.Ansi.Style.bold)
                .stackTraces (dev.jeka.core.tool.CommandLine.Help.Ansi.Style.italic)
                .applySystemProperties() // optional: allow end users to customize
                .build();
    }

    private static Map<String, String> cmdShortHand(JkProperties props) {
        return props.getAllStartingWith(JkConstants.CMD_PREFIX_PROP, false).entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("_"))
                .collect(Collectors.toMap(entry -> String.format("@|yellow ::%s|@", entry.getKey()),
                        Map.Entry::getValue));
    }
}
