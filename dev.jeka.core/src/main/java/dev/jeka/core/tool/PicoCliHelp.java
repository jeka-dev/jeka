package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.*;

class PicoCliHelp {

    private static final String SECTION_KBEANS_HEADING = "kbeanHeading";

    private static final String SECTION_KBEANS_DETAILS = "kbeanDetails";

    static CommandLine helpEntryPoint(ClassLoader classLoader,
                                      List<String> kbeanClassNames,
                                      String defaultKBeanClassName) {

        // Add commands and options from default KBean
        final CommandLine.Model.CommandSpec main;
        KBeanDescription beanDescription = null;
        if (defaultKBeanClassName != null) {
            Class<? extends KBean> defaultKBeanClass = JkClassLoader.of(classLoader).load(defaultKBeanClassName);
            beanDescription = KBeanDescription.of(defaultKBeanClass);
            main = PicocliCommands.fromKBeanDesc(beanDescription);
        } else {
            main = CommandLine.Model.CommandSpec.create().name("anonymous");
        }

        // Configure Usage
        main.usageMessage()
                .synopsisHeading("");
        List<String> synopsis = new LinkedList<>();

        CommandLine commandLine = new CommandLine(main);

        // -- Case defaultKBean is present
        if (defaultKBeanClassName != null) {
            synopsis.add(String.format("Default KBean: @|bold,yellow %s:|@ (%s)",
                    KBean.name(defaultKBeanClassName), defaultKBeanClassName));
            if (!JkUtilsString.isBlank(beanDescription.header)) {
                synopsis.add("Description  : " + beanDescription.header);
            }
            synopsis.add("Fields");
        }

        // Add section for other KBeans
        List<String> others = new LinkedList<>(kbeanClassNames);
        others.remove(defaultKBeanClassName);
        Map<String, Class<? extends KBean>> kbeanNameClassMap = beanNameClassMap(classLoader, others);
        Map<String, String> kbeans = new HashMap<>();
        for (Map.Entry<String, Class<? extends KBean>> entry : kbeanNameClassMap.entrySet()) {
            KBeanDescription description = KBeanDescription.of(entry.getValue());
            kbeans.put( String.format("@|bold,yellow %s:|@", entry.getKey()), description.header);
        }
        commandLine.getHelpSectionMap().put(SECTION_KBEANS_HEADING,
                help -> help.createHeading("\nAvailable KBeans\n"));
        commandLine.getHelpSectionMap().put(SECTION_KBEANS_DETAILS,
                help -> help.createTextTable(kbeans).toString());
        List<String> keys = new ArrayList<>(commandLine.getHelpSectionKeys());
        keys.add(SECTION_KBEANS_HEADING);
        keys.add(SECTION_KBEANS_DETAILS);
        commandLine.setHelpSectionKeys(keys);

        // Always present
        main.usageMessage()
                .width(150)
                .customSynopsis(synopsis.toArray(new String[0]))
                .commandListHeading("Methods:\n");
        commandLine.setPosixClusteredShortOptionsAllowed(false);
        return commandLine;

    }


    private static Map<String, Class<? extends KBean>> beanNameClassMap(ClassLoader classLoader,
                                                                        List<String> kbeanClasses) {
        Map<String, Class<? extends KBean>> result = new LinkedHashMap<>();
        kbeanClasses.stream().forEach(className -> {
            Class<? extends KBean> clazz = JkClassLoader.of(classLoader).load(className);
            result.put(KBean.name(className), clazz);
        });
        return result;
    }

    static CommandLine.Help.ColorScheme colorScheme() {
        return new CommandLine.Help.ColorScheme.Builder()
                .commands    (CommandLine.Help.Ansi.Style.bold, CommandLine.Help.Ansi.Style.fg_yellow)    // combine multiple styles
                .options     (CommandLine.Help.Ansi.Style.fg_yellow)                // yellow foreground color
                .parameters  (CommandLine.Help.Ansi.Style.fg_yellow)
                .optionParams(CommandLine.Help.Ansi.Style.italic)
                .errors      (CommandLine.Help.Ansi.Style.fg_red, CommandLine.Help.Ansi.Style.bold)
                .stackTraces (CommandLine.Help.Ansi.Style.italic)
                .applySystemProperties() // optional: allow end users to customize
                .build();
    }
}
