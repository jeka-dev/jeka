package org.jerkar.tool;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsThrowable;
import org.jerkar.api.utils.JkUtilsXml;
import org.jerkar.tool.PluginDictionary.PluginDescription;
import org.jerkar.tool.ProjectDef.BuildClassDef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

final class HelpDisplayer {

    private static final JkBuild DUMMY = new JkBuild();

    static void help(JkBuild build) {
        if (JkOptions.containsKey("plugins")) {
            helpPlugins();
            return;
        }
        JkLog.info("Usage: jerkar [methodA...] [-optionName=value...] [-DsystemPropName=value...]");
        JkLog.info("When no method specified, 'doDefault' method is invoked.");
        JkLog.info("Ex: jerkar javadoc compile -verbose=true -other=xxx -DmyProp=Xxxx");
        JkLog.nextLine();
        JkLog.info("Available methods and options :");
        BuildClassDef.of(build).log("", true);
        JkLog.nextLine();
        JkLog.info("Type 'jerkar help -plugins' to get help on plugins available in the classpath.");
        JkLog.nextLine();
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
        final Set<PluginDescription> pluginDescriptions = new PluginDictionary().getAll();
        for (final PluginDescription description : pluginDescriptions) {
            JkLog.nextLine();
            JkLog.info("Plugin class : " + description.fullName());
            JkLog.info("Plugin name  : " + description.shortName());
            final List<String> explanations = description.explanation();
            if (!explanations.isEmpty()) {
                JkLog.info("Purpose : " + description.explanation().get(0));
                JkLog.info(description.explanation().subList(1, description.explanation().size())
                        .stream().map(line -> "          " + line).collect(Collectors.toList()));
            }
            final Object object = JkUtilsReflect.newInstance(description.pluginClass(), JkBuild.class, DUMMY);
            BuildClassDef.of(object).log(description.shortName() + "#", false);
        }
    }

}
