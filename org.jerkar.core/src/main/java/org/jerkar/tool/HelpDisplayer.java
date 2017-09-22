package org.jerkar.tool;

import java.util.List;
import java.util.Set;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.tool.PluginDictionnary.JkPluginDescription;
import org.jerkar.tool.ProjectDef.ProjectBuildClassDef;

final class HelpDisplayer {

    public static void help(JkBuild build) {
        JkLog.info("Usage: jerkar [methodA...] [-optionName=value...] [-DsystemPropName=value...]");
        JkLog.info("When no method specified, then 'doDefault' method is processed.");
        JkLog.info("Ex: jerkar javadoc compile -verbose=true -other=xxx -DmyProp=Xxxx");
        JkLog.nextLine();
        // JkLog.infoUnderline("Build class availbale on this project");
        // JkProjectDef.of(build.baseDir().baseTree()).logAvailableBuildClasses();
        // JkLog.nextLine();
        // JkLog.infoString("To get details on a specific build class, type 'jerkar
        // help -buildClass=Xxxxxx");
        // JkLog.nextLine();
        JkLog.infoUnderlined("Help on build class " + build.getClass().getName());
        ProjectBuildClassDef.of(build).log(true);
        JkLog.nextLine();
        JkLog.info("Type 'jerkar helpPlugins' to get help on plugins");
        JkLog.nextLine();
    }

    public static void helpPlugins() {
        JkLog.startln("Looking for plugins");
        final Set<JkPluginDescription<JkBuildPlugin>> pluginDescriptions = PluginDictionnary.of(
                JkBuildPlugin.class).getAll();
        for (final JkPluginDescription<?> description : pluginDescriptions) {
            JkLog.nextLine();
            JkLog.infoHeaded("Plugin  Name : " + description.shortName());
            JkLog.info("Full name : " + description.fullName());
            JkLog.info("Template class : " + description.templateClass().getName());
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
