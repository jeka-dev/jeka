package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsThrowable;
import dev.jeka.core.api.utils.JkUtilsXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class HelpDisplayer {

    static void help(JkRuntime runtime) {
        if (JkOptions.containsKey("Plugins")) {
            helpPlugins(runtime);
            return;
        }
        StringBuilder sb = new StringBuilder()
                .append("Usage: \n\njeka (method | pluginName#method) [-optionName=<value>] [-pluginName#optionName=<value>] [-DsystemPropName=value]\n\n")
                .append("Executes the specified methods defined in Jeka class or plugins using the specified options and system properties.\n\n")
                .append("Ex: jeka clean java#pack -java#pack.sources=true -LogVerbose -other=xxx -DmyProp=Xxxx\n\n")
                .append(standardOptions());

        // List plugins
        final Set<BeanDictionary.KBeanDescription> KBeanDescriptions = new BeanDictionary().getAll();
        List<String> names = KBeanDescriptions.stream().map(KBeanDescription -> KBeanDescription.shortName()).collect(Collectors.toList());
        sb.append("\nAvailable plugins in classpath : ").append(String.join(", ", names))
                .append(".\n");

        sb.append("\nType 'jeka [pluginName]#help' to get help on a particular plugin (ex : 'jeka java#help'). ");
        sb.append("\nType 'jeka help -Plugins' to get help on all available plugins in the classpath.\n");
        JkLog.info(sb.toString());
    }

    private static String standardOptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("Built-in options (these options are not specific to a plugin or a Jeka class) :\n");
        sb.append("  -LogVerbose (shorthand -LV) : logs 'trace' level.\n");
        sb.append("  -LogQuiteVerbose (shorthand -LQV) : logs 'trace' level + Ivy trace level.\n");
        sb.append("  -LogRuntimeInfo (shorthand -LRI) : logs Jeka runtime information.\n");
        sb.append("  -LogBanner (shorthand -LB) : logs intro and outro banners.\n");
        sb.append("  -LogSetup (shorthand -LSU) : logs Jeka classes setup process.\n");
        sb.append("  -LogStyle (shorthand -LS) : choose the display log style : INDENT(default), BRACE, SQUARE or DEBUG.\n");
        sb.append("  -KBean (shorthand -KB) : Specify the default KBean in command line . It can be the short name of the class (without package prefix).\n");
        sb.append("  -ForceCompile (shorthand -FC) : Force Jeka to compile def files, even id def compiled classes are marked up-to-date.\n");
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

    private static void helpPlugins(JkRuntime runtime) {
        JkLog.info(helpPluginsDescription(runtime));
    }

    static void helpPlugin(JkBean jkBean) {
        final Set<BeanDictionary.KBeanDescription> KBeanDescriptions = new BeanDictionary().getAll();
        for (BeanDictionary.KBeanDescription beanDescription : KBeanDescriptions) {
            if (beanDescription.shortName().equals(jkBean.shortName())) {
                JkLog.info(helpPluginDescription(beanDescription, jkBean.getRuntime()));
                return;
            }
        }
    }

    private static String helpPluginsDescription(JkRuntime runtime) {
        final Set<BeanDictionary.KBeanDescription> KBeanDescriptions = new BeanDictionary().getAll();
        StringBuilder sb = new StringBuilder();
        for (final BeanDictionary.KBeanDescription description : KBeanDescriptions) {
            sb.append(helpPluginDescription(description, runtime));
        }
        return sb.toString();
    }

    private static String helpPluginDescription(BeanDictionary.KBeanDescription description, JkRuntime runtime) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nPlugin Class : " + description.fullName());
        sb.append("\nPlugin Name : " + description.shortName());
        List<String> deps = description.pluginDependencies();
        if (!deps.isEmpty()) {
            sb.append("\nDepends on plugins : " + String.join(", ", deps));
        }
        final List<String> explanations = description.explanation();
        if (!explanations.isEmpty()) {
            sb.append("\nPurpose : " + description.explanation().get(0));
            description.explanation().subList(1, description.explanation().size()).forEach(
                    line -> sb.append("\n          " + line));
        }
        final List<String> activationEffects = description.activationEffect();
        if (!activationEffects.isEmpty()) {
            sb.append("\nActivation Effects : " + description.activationEffect().get(0));
            description.explanation().subList(1, description.activationEffect().size()).forEach(
                    line -> sb.append("\n                      " + line));
        } else if (!description.isDecorateRunDefined()){
            sb.append("\nActivation Effect : None.");
        } else {
            sb.append("\nActivation Effect : Not documented.");
        }
        final JkBean bean;
        if (runtime.getBeanRegistry().getOptional(description.beanClass()).isPresent()) {
            bean = runtime.getBeanRegistry().get(description.beanClass());
        } else {
            bean = JkUtilsReflect.newInstance(description.beanClass());
        }
        sb.append("\n");
        sb.append(BeanDescription.of(description.beanClass()).flatDescription(description.shortName() + "#"));
        return sb.toString();
    }

    static List<String> optionValues(List<BeanDescription.BeanField> optionDefs) {
        return optionDefs.stream().map(optionDef -> optionDef.shortDescription()).collect(Collectors.toList());
    }

}
