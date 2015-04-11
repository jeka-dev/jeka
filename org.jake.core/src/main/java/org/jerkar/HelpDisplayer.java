package org.jerkar;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jerkar.PluginDictionnary.JakePluginDescription;
import org.jerkar.utils.JkUtilsReflect;
import org.jerkar.utils.JkUtilsString;

class HelpDisplayer {

	private static final int JUMP = 2;

	public static void help(JkBuild build) {
		final Class<?> buildClazz = build.getClass();
		JkLog.info("Usage: jake [methodA...] [-optionName=value...] [-DsystemPropName=value...]");
		JkLog.info("When no method specified, then 'base' method is processed.");
		JkLog.info("Ex: jake javadoc compile -verbose=true -other=xxx -DmyProp=Xxxx");
		JkLog.nextLine();
		JkLog.info("Available method(s) for '" + buildClazz.getName() + "' : " );
		displayHelpOnMethods(buildClazz);
		JkLog.nextLine();
		JkLog.info("Standard options for this build class : ");
		JkLog.nextLine();
		JkLog.shift(JUMP);
		JkLog.info(JkOptions.help(buildClazz));
		JkLog.shift(-JUMP);
		displayPlugins(build);
	}

	public static void helpPlugins(JkBuild build) {
		JkLog.startln("Looking for plugins");
		final Set<JakePluginDescription<JkBuildPlugin>> pluginDescriptions =
				PluginDictionnary.of(JkBuildPlugin.class).getAll();
		for (final JakePluginDescription<?> description : pluginDescriptions) {
			JkLog.nextLine();
			JkLog.info("Plugin  Name : " + description.shortName());
			JkLog.shift(4);
			JkLog.info("Full name : " + description.fullName());
			JkLog.info("Template class : " + description.templateClass().getName());
			final List<String> explanations = description.explanation();
			if (!explanations.isEmpty()) {
				JkLog.info("Explanation : ");
				JkLog.shift(2);
				JkLog.info(description.explanation());
				JkLog.shift(-2);
			}

			JkLog.info("Available method(s) for this plugin : " );
			displayHelpOnMethods(description.pluginClass());
			final List<String> optionHelp = JkOptions.helpClassOnly(description.pluginClass());
			if (optionHelp.isEmpty()) {
				JkLog.info("Available option(s) for this plugin : none.");
			} else {
				JkLog.info("Available option(s) for this plugin : ");
				JkLog.shift(JUMP);
				JkLog.info(optionHelp);
				JkLog.shift(-JUMP);
			}
			JkLog.shift(-4);
		}
		JkLog.done();
	}

	private static void displayPlugins(JkBuild build) {
		JkLog.startln("Looking for plugins");
		final List<JakePluginDescription<?>> pluginDescriptions = JakePluginDescription.declaredAsField(build);
		for (final JakePluginDescription<?> description : pluginDescriptions) {
			if (description.explanation() == null || description.explanation().isEmpty()) {
				JkLog.info();
			} else {
				JkLog.info("Found plugin : " + description + " : " + description.explanation().get(0));
			}
		}
		if (!pluginDescriptions.isEmpty()) {
			JkLog.info("To have more details about plugins, launch : jake helpPlugins.");
		}
		JkLog.done();
	}


	private static void displayHelpOnMethods(Class<?> target) {
		JkLog.shift(JUMP);
		final List<CommandDescription> list = new LinkedList<CommandDescription>();
		for (final Method method : target.getMethods()) {
			final int modifier = method.getModifiers();
			if (!method.getReturnType().equals(void.class)
					|| method.getParameterTypes().length != 0
					|| JkUtilsReflect.isMethodPublicIn(Object.class, method.getName())
					|| Modifier.isAbstract(modifier) || Modifier.isStatic(modifier)) {
				continue;
			}
			final JkDoc jkDoc = JkUtilsReflect.getInheritedAnnotation(method, JkDoc.class);
			if (jkDoc == null) {
				continue;
			}
			final CommandDescription actionDescription = new CommandDescription(method, jkDoc.value());
			list.add(actionDescription);
		}
		CommandDescription.log(list);
		if (list.isEmpty()) {
			JkLog.info("none");
		}
		JkLog.shift(-JUMP);
	}



	private static class CommandDescription implements Comparable<CommandDescription> {

		private final String name;
		private final String[] docs;
		private final Class<?> declaringClass;

		public CommandDescription(Method method, String[] docs) {
			super();
			this.name = method.getName();
			this.docs = Arrays.copyOf(docs, docs.length);
			this.declaringClass = method.getDeclaringClass();
		}

		@Override
		public int compareTo(CommandDescription other) {
			if (this.declaringClass.equals(other.declaringClass)) {
				return this.name.compareTo(other.name);
			}
			if (this.declaringClass.isAssignableFrom(other.declaringClass)) {
				return -1;
			}
			return 1;
		}

		public void log() {
			if (docs == null || docs.length == 0) {
				JkLog.info(name + " : Not documented.");
			} else if (docs.length == 1) {
				JkLog.info(name + " : " + docs[0]);
			} else {
				final String intro = name + " : ";
				JkLog.info(intro + docs[0]);
				final String margin = JkUtilsString.repeat(" ", intro.length());
				for (int i = 1; i < docs.length; i++) {
					JkLog.info(margin + docs[i]);
				}
			}
		}

		public static void log(List<CommandDescription> actions) {
			Class<?> currentDecClass = null;
			Collections.sort(actions);
			for(final CommandDescription actionDescription : actions) {
				if (actionDescription.declaringClass != currentDecClass) {
					JkLog.info("From " + actionDescription.declaringClass.getName());
					currentDecClass = actionDescription.declaringClass;
				}
				JkLog.shift(1);
				actionDescription.log();
				JkLog.shift(-1);
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final CommandDescription other = (CommandDescription) obj;
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			return true;
		}
	}

}
