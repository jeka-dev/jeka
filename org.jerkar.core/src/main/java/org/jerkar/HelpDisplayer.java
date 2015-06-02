package org.jerkar;

import java.util.List;
import java.util.Set;

import org.jerkar.JkProjectDef.JkProjectBuildClassDef;
import org.jerkar.PluginDictionnary.JkPluginDescription;

class HelpDisplayer {

	public static void help(JkBuild build) {
		JkLog.info("Usage: jerkar [methodA...] [-optionName=value...] [-DsystemPropName=value...]");
		JkLog.info("When no method specified, then 'base' method is processed.");
		JkLog.info("Ex: jerkar javadoc compile -verbose=true -other=xxx -DmyProp=Xxxx");
		JkLog.nextLine();
		JkProjectBuildClassDef.of(build.getClass()).log(true);
		JkLog.nextLine();
		JkLog.info("Type : 'jerkar helpPlugins' to get help on plugins");
	}

	public static void helpPlugins(JkBuild build) {
		JkLog.startln("Looking for plugins");
		final Set<JkPluginDescription<JkBuildPlugin>> pluginDescriptions =
				PluginDictionnary.of(JkBuildPlugin.class).getAll();
		for (final JkPluginDescription<?> description : pluginDescriptions) {
			JkLog.nextLine();
			JkLog.infoHead("Plugin  Name : " + description.shortName());
			JkLog.info("Full name : " + description.fullName());
			JkLog.info("Template class : " + description.templateClass().getName());
			final List<String> explanations = description.explanation();
			if (!explanations.isEmpty()) {
				JkLog.info("Explanation : ");
				JkLog.info(description.explanation());
			}
			JkLog.offset(2);
			JkProjectBuildClassDef.of(description.pluginClass()).log(false);
			JkLog.offset(-2);
		}
		JkLog.nextLine();
		JkLog.done();
	}



}
