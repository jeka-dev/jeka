package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

final class HelpDisplayer {

    static void help(List<Class<? extends JkBean>> localBeanClasses, List<Class> classpathBeanClasses,
                     boolean compilationFailed) {
        final StringBuilder introSb = new StringBuilder()
                .append("PURPOSE\n")
                .append("  Executes the specified methods defined in KBeans, using the specified properties, options and extra classpath.\n\n")
                .append("USAGE\n")
                .append("  jeka (method | kbean#method ...) [property=<value> | kbean#property=<value> ...] ")
                .append("[-option | -option=<value> ...] [@<module coordinates> ...] [@<path> ...] ")
                .append("[-DsystemPropertyName=<value> ...]\n\n")
                .append("EXAMPLE\n")
                .append("  jeka clean project#pack project#pack.sources=true -ls=DEBUG -Dmy.prop=aValue @org.example:a-plugin:1.1.0\n\n")
                .append(standardOptions());
        System.out.println(introSb.toString());

        final StringBuilder sb = new StringBuilder().append("LOCAL KBEANS\n");
        if (compilationFailed) {
            sb.append("  [WARN] Compilation of jeka/def failed. Cannot provide information about KBean defined locally.\n");
        } else {
            for (int i = 0; i < localBeanClasses.size(); i++) {
                sb.append(beanDescription(localBeanClasses.get(i), i== 0));
            }
        }

        // Global KBeans
        sb.append("\nGLOBAL KBEANS\n");
        classpathBeanClasses.stream()
                .sorted(Comparator.comparing(Class::getSimpleName))
                .forEach(aClass -> sb.append(beanDescription(aClass, false)));
        sb.append("\nType 'jeka [kbean]#help' to get help on a particular KBean (ex : 'jeka project#help'). ");
        System.out.println(sb.toString());
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

    private static String standardOptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("OPTIONS\n");
        sb.append("  -help (shorthand -h) : display this message.\n");
        sb.append("  -log.style (shorthand -ls) : choose the display log style : INDENT(default), BRACE or DEBUG.\n");
        sb.append("  -log.verbose (shorthand -lv) : log 'trace' level.\n");
        sb.append("  -log.ivy.verbose (shorthand -liv) : log 'trace' level + Ivy trace level.\n");
        sb.append("  -log.no.animation (shorthand -lna) : do not log animations on console.");
        sb.append("  -log.runtime.info (shorthand -lri) : log Jeka runtime information as Jeka version, JDK version, working dir, classpath ....\n");
        sb.append("  -log.banner (shorthand -lb) : log intro and outro banners.\n");
        sb.append("  -log.stacktrace (shorthand -lst) : log the stacktrace when Jeka fail.\n");
        sb.append("  -log.setup (shorthand -lsu) : log KBean setup process.\n");;
        sb.append("  -kbean (shorthand -kb) : Specify the default KBean in command line. It can be its name, its simple class name or its fully qualified class name.\n");
        sb.append("  -work.clean (shorthand -wc) : Delete all files cached in jeka/.work.\n");
        sb.append("  -def.compile.ignore-failure (shorthand -dci) : Try to compile def classes. If fail, ignore failure and continue.\n");
        return sb.toString();
    }

    static void help(Class<? extends JkBean> beanClass, Path xmlFile) {
        final Document document = JkUtilsXml.createDocument();
        final Element runEl = BeanDescription.of(beanClass).toElement(document);
        document.appendChild(runEl);
        if (xmlFile == null) {
            JkUtilsXml.output(document, System.out);
        } else {
            JkUtilsPath.createFile(xmlFile);
            try (final OutputStream os = Files.newOutputStream(xmlFile)) {
                JkUtilsXml.output(document, os);
            } catch (final IOException e) {
                throw JkUtilsThrowable.unchecked(e);
            }
            JkLog.info("Xml help file generated at " + xmlFile);
        }
    }

    static void helpJkBean(JkBean jkBean) {
        BeanDoc beanDescription = new BeanDoc(jkBean.getClass());
        JkLog.info(helpBeanDescription(beanDescription, jkBean.getRuntime()));
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
        final JkBean bean;
        if (runtime.getBeanOptional(description.beanClass()).isPresent()) {
            bean = runtime.getBean(description.beanClass());
        } else {
            bean = JkUtilsReflect.newInstance(description.beanClass());
        }
        sb.append(BeanDescription.of(description.beanClass()).flatDescription(description.shortName() + "#"));
        return sb.toString();
    }

    static List<String> propertyValues(List<BeanDescription.BeanField> propertyDescriptions) {
        return propertyDescriptions.stream().map(optionDef -> optionDef.shortDescription()).collect(Collectors.toList());
    }

}
