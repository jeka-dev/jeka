package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

final class HelpDisplayer {

    static void help(List<Class<? extends KBean>> localBeanClasses, List<Class> classpathBeanClasses,
                     boolean compilationFailed, Path baseDir) {
        String introSb = "\nPurpose:\n" +
                "  Executes the specified methods defined in KBeans, using the specified properties, options and extra classpath.\n\n" +
                "Usage:\n" +
                "  jeka (method | kbean#method ...) [property=<value> | kbean#property=<value> ...] " +
                "[-option | -option=<value> ...] [@<module coordinates> ...] [@<path> ...] " +
                "[-DsystemPropertyName=<value> ...]\n\n" +
                "Example:\n" +
                "  jeka project#clean project#pack project#pack.sources=true -ls=DEBUG -Dmy.prop=aValue @org.example:a-plugin:1.1.0\n\n" +
                standardProperties();
        System.out.println(introSb);

        final StringBuilder sb = new StringBuilder().append("Local KBeans:\n");
        if (compilationFailed) {
            sb.append("  [WARN] Compilation of jeka/def failed. Cannot provide information about KBean defined locally.\n");
        } else {
            List<RenderItem> renderItems = new LinkedList<>();
            for (int i = 0; i < localBeanClasses.size(); i++) {
                renderItems.add(bean(localBeanClasses.get(i), i == 0));
            }
            new ItemContainer(renderItems).render().forEach(line -> sb.append("  " + line + "\n"));
        }

        // Global KBeans
        sb.append("\nClasspath KBeans:\n");
        List<RenderItem> renderItems = classpathBeanClasses.stream()
                .sorted(Comparator.comparing(Class::getSimpleName))
                .map(beanClass -> bean(beanClass, false))
                .collect(Collectors.toList());
        new ItemContainer(renderItems).render().forEach(line -> sb.append("  " + line + "\n"));
        sb.append("\nType 'jeka [kbean]#help' to get help on a particular KBean (ex : 'jeka project#help').\n");
        System.out.println(sb);
    }

    private static RenderItem bean(Class beanClass, boolean isDefault) {
        String shortName = KBean.name(beanClass);
        if (isDefault) {
            shortName = shortName + " (default)";
        }
        String beanDescription = new BeanDoc(beanClass).shortDescription();
        if (JkUtilsString.isBlank(beanDescription)) {
            beanDescription = "";
        } else if (!beanDescription.endsWith(".")) {
            beanDescription = beanDescription + ".";
        }
        beanDescription = beanDescription.trim();
        beanDescription = beanDescription + "[" + beanClass.getName() + "]";
        return new RenderItem(shortName, Collections.singletonList(beanDescription));
    }

    private static String standardProperties() {
        StringBuilder sb = new StringBuilder();
        sb.append("Options:\n");
        List<RenderItem> items = new LinkedList<>();
        items.add(option("help", "h", "display this message"));
        items.add(option("log.style", "ls", "choose the display log style : INDENT(default), BRACE or DEBUG"));
        items.add(option("log.verbose", "lv", "log 'trace' level"));
        items.add(option("log.ivy.verbose", "liv",  " log 'trace' level + Ivy trace level"));
        items.add(option("log.runtime.information", "lri",  " log Jeka runbase information at startup"));
        items.add(option("log.no.animation", "lna",  " do not log animations on console"));
        items.add(option("log.duration", "ld",  " log execution duration"));
        items.add(option("log.runtime.info", "lri",  " log Jeka runbase information as Jeka version, JDK version, working dir, classpath ..."));
        items.add(option("log.banner", "lb",  " log intro and outro banners"));
        items.add(option("log.stacktrace", "lst",  " log the stacktrace when Jeka fail"));
        items.add(option("log.setup", "lsu",  " log KBean setup process"));
        items.add(option("kbean", Environment.KB_KEYWORD, " Specify the default KBean in command line. It can be its name, its simple class name or its fully qualified class name"));
        items.add(option("clean.work", "cw",  " Delete all files cached in jeka/.work"));
        items.add(option("no.help", "", "Does not display help if no method is invoked"));
        items.add(option("def.compile.ignore-failure", "dci",  " Try to compile def classes. If fail, ignore failure and continue"));
        new ItemContainer(items).render().forEach(item -> sb.append("  " + item + "\n"));
        return sb.toString();
    }

    private static RenderItem option(String name, String shortHand, String desc) {
        String key = "-" + name;
        if (!JkUtilsString.isBlank(shortHand)) {
            key = key + " (shorthand -" + shortHand + ")";
        }
        return new RenderItem(key, Collections.singletonList(JkUtilsString.capitalize(desc.trim())));
    }

    private static String shortcuts(Path baseDir) {
        StringBuilder sb = new StringBuilder();
        Map<String, String> props = JkExternalToolApi.getCmdShortcutsProperties(baseDir);
        if (props.isEmpty()) {
            return "";
        }
        sb.append("\nCommand Shortcuts:\n");
        int maxLength = Collections.max(props.keySet().stream().map(String::length).collect(Collectors.toSet()));

        for (Map.Entry<String, String> entry : props.entrySet()) {
            sb.append("  :" + JkUtilsString.padEnd(entry.getKey(), maxLength + 2, ' ') + ": " + entry.getValue() + "\n");
        }
        return sb.toString();
    }

    static void helpJkBean(KBean kBean) {
        BeanDoc beanDescription = new BeanDoc(kBean.getClass());
        JkLog.info(helpBeanDescription(beanDescription, kBean.getRunbase()));
        JkLog.info("Execute 'jeka -help' to get global help.\n");
    }

    private static String helpBeanDescription(BeanDoc description, JkRunbase runbase) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        List<RenderItem> items = new LinkedList<>();
        items.add(RenderItem.of("Class", description.fullName()));
        items.add(RenderItem.of("Name", description.shortName()));
        List<String> deps = description.pluginDependencies();
        if (!deps.isEmpty()) {
            items.add(new RenderItem("KBean Dependencies", deps));
        }
        final List<String> explanations = description.description();
        if (!explanations.isEmpty()) {
            items.add(new RenderItem("Purpose", explanations));
        }
        final List<String> activationEffects = description.activationEffect();
        if (!activationEffects.isEmpty()) {
            items.add(new RenderItem("Instantiation effects", activationEffects));
        }
        new ItemContainer(items).render().forEach(line -> sb.append(line + "\n"));
        sb.append(BeanDescription.renderItem(description.beanClass()).flatDescription());
        sb.append(shortcuts(runbase.getBaseDir()));
        return sb.toString();
    }

    static class RenderItem {

        private static final String SEPARATOR = " : ";

        private final String name;

        private final List<String> lines;

        RenderItem(String name, List<String> lines) {
            this.name = name;
            this.lines = lines;
        }

        static RenderItem of(String name, String line) {
            return new RenderItem(name, Collections.singletonList(line));
        }

        static List<String> split(String line) {
            return Arrays.stream(line.split("\\.\\s|\\r?\\n"))
                    .map(l -> l.replace('\r', ' ').trim())
                    .filter(l -> !JkUtilsString.isBlank(l))
                    .map(l -> l.endsWith(".") ? l : l + ".")
                    .collect(Collectors.toList());
        }

        List<String> render(int maxNameSize) {
            String paddedName = JkUtilsString.padEnd(name, maxNameSize, ' ');
            List<String> result = new LinkedList<>();
            result.add(paddedName + SEPARATOR + lines.get(0));
            String margin = JkUtilsString.repeat(" ", maxNameSize);
            margin = margin + JkUtilsString.repeat(" ", SEPARATOR.length());
            for (int i = 1; i < lines.size(); i++) {
                result.add(margin + lines.get(i));
            }
            return result;
        }
    }

    static class ItemContainer {

        private final List<RenderItem> items;

        ItemContainer(List<RenderItem> items) {
            this.items = items;
        }

        List<String> render() {
            int maxNameSize = maxLength(items.stream().map(item -> item.name).collect(Collectors.toList()));
            List<String> result = new LinkedList<>();
            for (RenderItem item : items) {
                result.addAll(item.render(maxNameSize));
            }
            return result;
        }

        private static int maxLength(List<String> lines) {
            if (lines.isEmpty()) {
                return 0;
            }
            return Collections.max(lines.stream().filter(o -> o != null).collect(Collectors.toList()),
                    Comparator.comparing(String::length)).length();
        }

    }

}
