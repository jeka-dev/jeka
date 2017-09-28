package org.jerkar.tool;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsThrowable;
import org.jerkar.api.utils.JkUtilsXml;
import org.jerkar.tool.PluginDictionnary.JkPluginDescription;
import org.jerkar.tool.ProjectDef.ProjectBuildClassDef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

final class HelpDisplayer {

    static void help(JkBuild build) {
        JkLog.info("Usage: jerkar [methodA...] [-optionName=value...] [-DsystemPropName=value...]");
        JkLog.info("When no method specified, then 'doDefault' method is processed.");
        JkLog.info("Ex: jerkar javadoc compile -verbose=true -other=xxx -DmyProp=Xxxx");
        JkLog.nextLine();
        JkLog.infoUnderlined("Help on build class " + build.getClass().getName());
        ProjectBuildClassDef.of(build).log(true);
        JkLog.nextLine();
        JkLog.info("Type 'jerkar helpPlugins' to get help on plugins");
        JkLog.nextLine();
    }

    static void help(JkBuild build, Path xmlFile) {
        final Document document = JkUtilsXml.createDocument();
        final Element buildEl = ProjectDef.ProjectBuildClassDef.of(build).toElement(document);
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
        JkLog.startln("Looking for plugins");
        final Set<JkPluginDescription> pluginDescriptions = new PluginDictionnary().getAll();
        for (final JkPluginDescription description : pluginDescriptions) {
            JkLog.nextLine();
            JkLog.infoHeaded("Plugin  Name : " + description.shortName());
            JkLog.info("Full name : " + description.fullName());
            final List<String> explanations = description.explanation();
            if (!explanations.isEmpty()) {
                JkLog.info("Explanation : ");
                JkLog.info(description.explanation());
            }
            JkLog.delta(2);
            final Object object = JkUtilsReflect.newInstance(description.pluginClass());
            ProjectBuildClassDef.of(object).log(false, description.shortName() + "#");
            JkLog.delta(-2);
        }
        JkLog.nextLine();
        JkLog.done();
    }

}
