package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

class PicoCliHelp {

    private static final String SECTION_OTHER_KBEANS_HEADING = "otherKBeanHeading";

    private static final String SECTION_OTHER_KBEANS_DETAILS = "otherKbeanDetails";

    private static final String SECTION_STD_KBEANS_HEADING = "stdKBeanHeading";

    private static final String SECTION_STD_KBEANS_DETAILS = "stdKbeanDetails";

    static void printUsageHelp(PrintStream printStream) {
        CommandLine commandLine = PicocliCommands.mainCommandLine();
        try {
            PicocliCommands.STANDARD_KBEAN_CLASSES.forEach(kbeanClass -> {
                CommandLine.Model.CommandSpec kbeanSpec = simpleFromKbeanClass(kbeanClass);
                commandLine.addSubcommand(kbeanSpec);
            });
        } catch (Exception e) {  // Help should be robust
            printStream.println("Error while processing help.");
            e.printStackTrace(printStream);
        } finally {
            commandLine.usage(printStream, colorScheme());
        }
    }

    static void printVersionHelp(PrintStream printStream) {
        CommandLine commandLine = PicocliCommands.stdHelp();
        commandLine.printVersionHelp(printStream);
    }

    static void printCommandHelp(JkPathSequence classpath,
                                 List<String> kbeanClassNames,
                                 String defaultKBeanClassName,
                                 PrintStream printStream) {
        commandHelp(classpath, kbeanClassNames, defaultKBeanClassName).usage(printStream, colorScheme());
    }

    // return false if kbeanName does not math any KBean
    static boolean printKBeanHelp(JkPathSequence classpath,
                                 List<String> kbeanClassNames,
                                 String kbeanName,
                                 PrintStream printStream) {
        CommandLine commandLine = kbeanHelp(classpath, kbeanClassNames, kbeanName);
        if (commandLine == null) {
            return false;
        }
        commandLine.usage(printStream, colorScheme());
        return true;
    }

    private static CommandLine commandHelp(JkPathSequence classpath,
                                   List<String> kbeanClassNames,
                                   String defaultKBeanClassName) {

        ClassLoader classLoader = JkUrlClassLoader.of(classpath).get();

        // Add commands and options from default KBean
        final CommandLine.Model.CommandSpec main;
        KBeanDescription beanDescription = null;
        if (defaultKBeanClassName != null) {
            Class<? extends KBean> defaultKBeanClass = JkClassLoader.of(classLoader).load(defaultKBeanClassName);
            beanDescription = KBeanDescription.of(defaultKBeanClass);
            main = PicocliCommands.fromKBeanDesc(beanDescription);
        } else {
            main = dev.jeka.core.tool.CommandLine.Model.CommandSpec.create().name("anonymous");
        }

        // Configure Usage
        main.usageMessage().synopsisHeading("");
        List<String> synopsis = new LinkedList<>();

        // -- Case defaultKBean is present
        if (defaultKBeanClassName != null) {
            synopsis.add(String.format("Default KBean @|yellow %s:|@ (%s)",
                    KBean.name(defaultKBeanClassName), defaultKBeanClassName));
            if (!JkUtilsString.isBlank(beanDescription.header)) {
                synopsis.add("Description  : " + beanDescription.header);
            }
            synopsis.add("\nFields");
        }
        main.usageMessage()
                .autoWidth(true)
                .customSynopsis(synopsis.toArray(new String[0]))
                .commandListHeading("Methods\n");

        // Add section for Standard KBeans
        Map<String, String> stdKBeans = new LinkedHashMap<>();
        for (Class<? extends KBean> kbeanClass : PicocliCommands.STANDARD_KBEAN_CLASSES) {
            KBeanDescription description = KBeanDescription.of(kbeanClass);
            String name = KBean.name(kbeanClass);
            stdKBeans.put( String.format("@|yellow %s:|@", name), description.header);
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
        List<String> others = new LinkedList<>(kbeanClassNames);
        List<String> stdKbeanClassNames = PicocliCommands.STANDARD_KBEAN_CLASSES.stream()
                        .map(Class::getName).collect(Collectors.toList());
        others.removeAll(stdKbeanClassNames);
        others.remove(defaultKBeanClassName);
        if (!others.isEmpty()) {
            Map<String, Class<? extends KBean>> kbeanNameClassMap = beanNameClassMap(classLoader, others);
            Map<String, String> kbeans = new HashMap<>();
            for (Map.Entry<String, Class<? extends KBean>> entry : kbeanNameClassMap.entrySet()) {
                KBeanDescription description = KBeanDescription.of(entry.getValue());
                kbeans.put(String.format("@|yellow %s:|@", entry.getKey()), description.header);
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

        main.usageMessage()
                .footer("", "Execute @|yellow jeka -cmd=<kbean>|@ (as @|italic jeka -cmd=docker|@) " +
                        "to get details on specific KBean.");

        return commandLine;

    }

    private static CommandLine kbeanHelp(JkPathSequence classpath,
                                   List<String> kbeanClassNames,
                                   String kbeanName) {

        String kbeanClassName = kbeanClassNames.stream()
                .filter(clazzName -> KBean.nameMatches(clazzName, kbeanName))
                .findFirst().orElse(null);
        if (kbeanClassName == null) {
            return null;
        }

        ClassLoader classLoader = JkUrlClassLoader.of(classpath).get();
        Class<? extends KBean> defaultKBeanClass = JkClassLoader.of(classLoader).load(kbeanClassName);
        KBeanDescription beanDescription = KBeanDescription.of(defaultKBeanClass);
        CommandLine.Model.CommandSpec main = PicocliCommands.fromKBeanDesc(beanDescription);

        // Configure Usage
        main.usageMessage().synopsisHeading("");
        List<String> synopsis = new LinkedList<>();
        synopsis.add(String.format("KBean @|yellow %s:|@ (%s)",
                    KBean.name(kbeanName), kbeanClassName));
        synopsis.add("");
        if (!JkUtilsString.isBlank(beanDescription.header)) {
            String header = beanDescription.header.trim();
            header = header.endsWith(".") ? header : header + ".";
            synopsis.add(header);
            List<String> descLines = Arrays.asList(beanDescription.description.split("\n"));
            synopsis.addAll(descLines);
            if (!descLines.isEmpty()) {
                synopsis.add("");
            }
        }
        synopsis.add("Fields");
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
            Class<? extends KBean> clazz = JkClassLoader.of(classLoader).load(className);
            result.put(KBean.name(className), clazz);
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

    private static CommandLine.Model.CommandSpec simpleFromKbeanClass(Class<? extends KBean> kbeanClass) {
        KBeanDescription kBeanDescription = KBeanDescription.of(kbeanClass);
        CommandLine.Model.CommandSpec commandSpec = dev.jeka.core.tool.CommandLine.Model.CommandSpec.create();
        commandSpec.name(KBean.name(kbeanClass) + ":");
        commandSpec.usageMessage().header(kBeanDescription.header);
        return commandSpec;
    }
}
