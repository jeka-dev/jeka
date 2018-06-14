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
import org.jerkar.tool.ProjectDef.BuildClassDef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

final class HelpDisplayer {

    private static final JkBuild DUMMY = JkUtilsReflect.newInstance(JkBuild.class);

    static void help(JkBuild build) {
        if (JkOptions.containsKey("plugins")) {
            helpPlugins();
            return;
        }
        StringBuilder sb = new StringBuilder()
                .append("Usage: jerkar [methodA...] [-optionName=value...] [-DsystemPropName=value...]\n")
                .append("When no method specified, 'doDefault' method is invoked.\n")
                .append("Ex: jerkar javadoc compile -verbose=true -other=xxx -DmyProp=Xxxx\n\n")
                .append("Available methods and options :\n");

        sb.append(BuildClassDef.of(build).description("", true));
        sb.append("\nType 'jerkar help -plugins' to get help on plugins available in the classpath.\n");
        JkLog.info(sb.toString());
    }

    static void help(JkBuild build, Path xmlFile) {
        final Document document = JkUtilsXml.createDocument();
        final Element buildEl = BuildClassDef.of(build).toElement(document);
        document.appendChild(buildEl);
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

    static void helpPlugins() {
        JkLog.info(helpPluginsDescription());
    }

    private static String helpPluginsDescription() {
        final Set<PluginDescription> pluginDescriptions = new PluginDictionary().getAll();
        StringBuilder sb = new StringBuilder();
        for (final PluginDescription description : pluginDescriptions) {
            sb.append("\nPlugin class : " + description.fullName());
            sb.append("\nPlugin name : " + description.shortName());
            List<String> deps = description.pluginDependencies();
            if (!deps.isEmpty()) {
                sb.append("Depends on plugins : " + JkUtilsString.join(deps, ", "));
            }
            final List<String> explanations = description.explanation();
            if (!explanations.isEmpty()) {
                sb.append("\nPurpose : " + description.explanation().get(0));
                description.explanation().subList(1, description.explanation().size()).forEach(
                        line -> sb.append("\n          " + line));
            }
            final List<String> activationEffects = description.activationEffect();
            if (!activationEffects.isEmpty()) {
                sb.append("\nActivation effects : " + description.activationEffect().get(0));
                description.explanation().subList(1, description.activationEffect().size()).forEach(
                        line -> sb.append("\n                      " + line));
            } else if (!description.isDecorateBuildDefined()){
                sb.append("\nActivation effects : None.");
            } else {
                sb.append("\nActivation effects : Not documented.");
            }
            final Object object = JkUtilsReflect.newInstance(description.pluginClass(), JkBuild.class, DUMMY);
            sb.append("\n");
            sb.append(BuildClassDef.of(object).description(description.shortName() + "#", false));
        }
        return sb.toString();
    }

    static List<String> optionValues(List<ProjectDef.BuildOptionDef> optionDefs) {
        return optionDefs.stream().map(optionDef -> optionDef.shortDescription()).collect(Collectors.toList());
    }

}
