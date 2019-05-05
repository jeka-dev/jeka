package org.jerkar.tool;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.*;
import org.jerkar.tool.PluginDictionary.PluginDescription;
import org.jerkar.tool.ProjectDef.RunClassDef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

final class HelpDisplayer {

    static void help(JkRun jkRun) {
        if (JkOptions.containsKey("Plugins")) {
            helpPlugins(jkRun);
            return;
        }
        StringBuilder sb = new StringBuilder()
                .append("Usage: \njerkar (method | pluginName#method) [-optionName=<value>] [-pluginName#optionName=<value>] [-DsystemPropName=value]\n\n")
                .append("Execute the specified methods defined in run class or plugins using the specified options and ofSystem properties.\n")
                .append("Ex: jerkar clean java#pack -java#pack.sources=true -LogVerbose -other=xxx -DmyProp=Xxxx\n\n")
                .append(standardOptions())
                .append("\nAvailable methods and options :\n")
                .append(RunClassDef.of(jkRun).description("", true));

        // List plugins
        final Set<PluginDescription> pluginDescriptions = new PluginDictionary().getAll();
        List<String> names = pluginDescriptions.stream().map(pluginDescription -> pluginDescription.shortName()).collect(Collectors.toList());
        sb.append("\nAvailable plugins in classpath : ").append(JkUtilsString.join(names, ", "))
                .append(".\n");

        sb.append("\nType 'jerkar [pluginName]#help' to get help on a perticular plugin (ex : 'jerkar java#help'). ");
        sb.append("\nType 'jerkar help -Plugins' to get help on all available plugins in the classpath.\n");
        JkLog.info(sb.toString());
    }

    static String standardOptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("Built-in options (these options are not specific to a plugin or a run class) :\n");
        sb.append("  -LogVerbose (shorthand -LV) : if true, logs will display 'trace' level logs.\n");
        sb.append("  -LogHeaders (shorthand -LH) : if true, meta-information about the run creation itself and method execution will be logged.\n");
        sb.append("  -LogMaxLength (shorthand -LML) : Console will do a carriage return automatically after N characters are outputted in a single line (ex : -LML=120).\n");
        sb.append("  -RunClass (shorthand -RC) : Force to use the specified class as the run class to invoke. It can be the short name of the class (without package prefix).\n");
        return sb.toString();
    }

    static void help(JkRun run, Path xmlFile) {
        final Document document = JkUtilsXml.createDocument();
        final Element runEl = RunClassDef.of(run).toElement(document);
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

    static void helpPlugins(JkRun jkRun) {
        JkLog.info(helpPluginsDescription(jkRun));
    }

    static void helpPlugin(JkPlugin plugin) {
        final Set<PluginDescription> pluginDescriptions = new PluginDictionary().getAll();
        for (PluginDescription pluginDescription : pluginDescriptions) {
            if (pluginDescription.shortName().equals(plugin.name())) {
                JkLog.info(helpPluginDescription(plugin.getRun(), pluginDescription));
                return;
            }
        }
    }

    private static String helpPluginsDescription(JkRun jkRun) {
        final Set<PluginDescription> pluginDescriptions = new PluginDictionary().getAll();
        StringBuilder sb = new StringBuilder();
        for (final PluginDescription description : pluginDescriptions) {
            sb.append(helpPluginDescription(jkRun, description));
        }
        return sb.toString();
    }

    private static String helpPluginDescription(JkRun jkRun, PluginDescription description) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nPlugin Class : " + description.fullName());
        sb.append("\nPlugin Name : " + description.shortName());
        List<String> deps = description.pluginDependencies();
        if (!deps.isEmpty()) {
            sb.append("\nDepends on plugins : " + JkUtilsString.join(deps, ", "));
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
        final Object object = JkUtilsReflect.newInstance(description.pluginClass(), JkRun.class, jkRun);
        sb.append("\n");
        sb.append(RunClassDef.of(object).flatDescription(description.shortName() + "#", false));
        return sb.toString();
    }

    static List<String> optionValues(List<ProjectDef.RunOptionDef> optionDefs) {
        return optionDefs.stream().map(optionDef -> optionDef.shortDescription()).collect(Collectors.toList());
    }

}
