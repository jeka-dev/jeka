package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

final class HelpDisplayer {

    static void help(List<Class<? extends JkBean>> localBeanClasses, List<Class> classpathBeanClasses,
                     boolean compilationFailed, Path baseDir) {
        final StringBuilder introSb = new StringBuilder()
                .append("PURPOSE\n")
                .append("  Executes the specified methods defined in KBeans, using the specified properties, options and extra classpath.\n\n")
                .append("USAGE\n")
                .append("  jeka (method | kbean#method ...) [property=<value> | kbean#property=<value> ...] ")
                .append("[-option | -option=<value> ...] [@<module coordinates> ...] [@<path> ...] ")
                .append("[-DsystemPropertyName=<value> ...]\n\n")
                .append("EXAMPLE\n")
                .append("  jeka project#clean project#pack project#pack.sources=true -ls=DEBUG -Dmy.prop=aValue @org.example:a-plugin:1.1.0\n\n")
                .append(standardProperties());
        System.out.println(introSb);

        final StringBuilder sb = new StringBuilder().append("LOCAL KBEANS\n");
        if (compilationFailed) {
            sb.append("  [WARN] Compilation of jeka/def failed. Cannot provide information about KBean defined locally.\n");
        } else {
            for (int i = 0; i < localBeanClasses.size(); i++) {
                sb.append(beanDescription(localBeanClasses.get(i), i== 0));
            }
        }

        // Global KBeans
        sb.append("\nCLASSPATH KBEANS\n");
        classpathBeanClasses.stream()
                .sorted(Comparator.comparing(Class::getSimpleName))
                .forEach(aClass -> sb.append(beanDescription(aClass, false)));
        sb.append("\nType 'jeka [kbean]#help' to get help on a particular KBean (ex : 'jeka project#help'). ");
        System.out.println(sb);
    }

    private static String beanDescription(Class beanClass, boolean isDefault) {
        String shortName = JkBean.name(beanClass);
        if (isDefault) {
            shortName = shortName + " (default)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("  " + JkUtilsString.padEnd(shortName, 25, ' ') + ": ");
        String beanDescription = new BeanDoc(beanClass).shortDescription();
        if (beanDescription != null) {
            sb.append(beanDescription).append(" ");
        }
        sb.append("[" + beanClass.getName() + "]");
        sb.append("\n");
        return sb.toString();
    }

    private static String standardProperties() {
        StringBuilder sb = new StringBuilder();
        sb.append("OPTIONS\n");
        sb.append("  -help (shorthand -h) : display this message.\n");
        sb.append("  -log.style (shorthand -ls) : choose the display log style : INDENT(default), BRACE or DEBUG.\n");
        sb.append("  -log.verbose (shorthand -lv) : log 'trace' level.\n");
        sb.append("  -log.ivy.verbose (shorthand -liv) : log 'trace' level + Ivy trace level.\n");
        sb.append("  -log.runtime.information (shorthand -lri) : log Jeka runtime information at startup.\n");
        sb.append("  -log.no.animation (shorthand -lna) : do not log animations on console.");
        sb.append("  -log.duration (shorthand -ld) : log execution duration.");
        sb.append("  -log.runtime.info (shorthand -lri) : log Jeka runtime information as Jeka version, JDK version, working dir, classpath ....\n");
        sb.append("  -log.banner (shorthand -lb) : log intro and outro banners.\n");
        sb.append("  -log.stacktrace (shorthand -lst) : log the stacktrace when Jeka fail.\n");
        sb.append("  -log.setup (shorthand -lsu) : log KBean setup process.\n");;
        sb.append("  -kbean (shorthand -kb) : Specify the default KBean in command line. It can be its name, its simple class name or its fully qualified class name.\n");
        sb.append("  -clean.work (shorthand -cw) : Delete all files cached in jeka/.work.\n");
        sb.append("  -no.help : Does not display help if no method is invoked.\n");
        sb.append("  -def.compile.ignore-failure (shorthand -dci) : Try to compile def classes. If fail, ignore failure and continue.\n");
        return sb.toString();
    }

    private static String shortcuts(Path baseDir) {
        StringBuilder sb = new StringBuilder();
        Map<String, String> props = JkExternalToolApi.getCmdShortcutsProperties(baseDir);
        if (props.isEmpty()) {
            return "";
        }
        sb.append("\nCOMMAND SHORTCUTS\n");
        int maxLength = Collections.max(props.keySet().stream().map(String::length).collect(Collectors.toSet()));

        for (Map.Entry<String, String> entry : props.entrySet()) {
            sb.append("  :" + JkUtilsString.padEnd(entry.getKey(), maxLength + 2, ' ') + ": " + entry.getValue() + "\n");
        }
        return sb.toString();
    }

    static void helpJkBean(JkBean jkBean) {
        BeanDoc beanDescription = new BeanDoc(jkBean.getClass());
        JkLog.info(helpBeanDescription(beanDescription, jkBean.getRuntime()));
        JkLog.info("Execute 'jeka -help' to get global help.");
    }

    private static String helpBeanDescription(BeanDoc description, JkRuntime runtime) {
        StringBuilder sb = new StringBuilder();
        sb.append("CLASS\n  " + description.fullName() + "\n");
        sb.append("NAME\n  " + description.shortName() + "\n");
        List<String> deps = description.pluginDependencies();
        if (!deps.isEmpty()) {
            sb.append("DEPENDENCIES ON OTHER KBEANS\n");
            deps.forEach(dep -> sb.append("  " + dep + "\n"));
        }
        final List<String> explanations = description.description();
        if (!explanations.isEmpty()) {
            sb.append("PURPOSE\n");
            description.description().stream()
                    .flatMap(string -> Arrays.stream(string.split("\n")))
                    .forEach(line -> sb.append("  " + line + "\n"));
        }
        final List<String> activationEffects = description.activationEffect();
        if (!activationEffects.isEmpty()) {
            sb.append("INSTANTIATION EFFECT\n");
            description.activationEffect().stream()
                    .flatMap(string -> Arrays.stream(string.split("\n")))
                    .forEach(line -> sb.append("  " + line + "\n"));
        }
        sb.append(BeanDescription.of(description.beanClass()).flatDescription(description.shortName() + "#"));
        sb.append(shortcuts(runtime.getProjectBaseDir()));
        return sb.toString();
    }



}
