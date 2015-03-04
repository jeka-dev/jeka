package org.jake;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jake.PluginDictionnary.JakePluginDescription;
import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;

class HelpDisplayer {

	private static final int JUMP = 2;

	public static void help(JakeBuild build) {
		final Class<?> buildClazz = build.getClass();
		JakeLog.info("Usage: jake [methodA...] [-optionName=value...] [-DsystemPropName=value...]");
		JakeLog.info("When no method specified, then 'base' method is processed.");
		JakeLog.info("Ex: jake javadoc compile -verbose=true -other=xxx -DmyProp=Xxxx");
		JakeLog.nextLine();
		JakeLog.info("Available method(s) for '" + buildClazz.getName() + "' : " );
		displayHelpOnMethods(buildClazz);
		JakeLog.nextLine();
		JakeLog.info("Standard options for this build class : ");
		JakeLog.nextLine();
		JakeLog.shift(JUMP);
		JakeLog.info(JakeOptions.help(buildClazz));
		JakeLog.shift(-JUMP);
		displayPlugins(build);
	}

	public static void helpPlugins(JakeBuild build) {
		JakeLog.startln("Looking for plugins");
		final List<JakePluginDescription<?>> pluginDescriptions = JakePluginDescription.declaredAsField(build);
		for (final JakePluginDescription<?> description : pluginDescriptions) {
			JakeLog.nextLine();
			JakeLog.info("Plugin  Name : " + description.shortName());
			JakeLog.shift(4);
			JakeLog.info("Full name : " + description.fullName());
			JakeLog.info("Template class : " + description.templateClass().getName());
			final List<String> explanations = description.explanation();
			if (!explanations.isEmpty()) {
				JakeLog.info("Explanation : ");
				JakeLog.shift(2);
				JakeLog.info(description.explanation());
				JakeLog.shift(-2);
			}

			JakeLog.info("Available method(s) for this plugin : " );
			displayHelpOnMethods(description.pluginClass());
			final List<String> optionHelp = JakeOptions.helpClassOnly(description.pluginClass());
			if (optionHelp.isEmpty()) {
				JakeLog.info("Available option(s) for this plugin : none.");
			} else {
				JakeLog.info("Available option(s) for this plugin : ");
				JakeLog.shift(JUMP);
				JakeLog.info(optionHelp);
				JakeLog.shift(-JUMP);
			}
			JakeLog.shift(-4);
		}
	}

	private static void displayPlugins(JakeBuild build) {
		JakeLog.startln("Looking for plugins");
		final List<JakePluginDescription<?>> pluginDescriptions = JakePluginDescription.declaredAsField(build);
		for (final JakePluginDescription<?> description : pluginDescriptions) {
			if (description.explanation() == null || description.explanation().isEmpty()) {
				JakeLog.info();
			} else {
				JakeLog.info("Found plugin : " + description + " : " + description.explanation().get(0));
			}
		}
		if (!pluginDescriptions.isEmpty()) {
			JakeLog.info("To have more details about plugins, launch : jake helpPlugins.");
		}
		JakeLog.done();
	}


	private static void displayHelpOnMethods(Class<?> target) {
		JakeLog.shift(JUMP);
		final List<CommandDescription> list = new LinkedList<CommandDescription>();
		for (final Method method : target.getMethods()) {
			final int modifier = method.getModifiers();
			if (!method.getReturnType().equals(void.class)
					|| method.getParameterTypes().length != 0
					|| JakeUtilsReflect.isMethodPublicIn(Object.class, method.getName())
					|| Modifier.isAbstract(modifier) || Modifier.isStatic(modifier)) {
				continue;
			}
			final JakeDoc jakeDoc = JakeUtilsReflect.getInheritedAnnotation(method, JakeDoc.class);
			if (jakeDoc == null) {
				continue;
			}
			final CommandDescription actionDescription = new CommandDescription(method, jakeDoc.value());
			list.add(actionDescription);
		}
		CommandDescription.log(list);
		if (list.isEmpty()) {
			JakeLog.info("none");
		}
		JakeLog.shift(-JUMP);
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
				JakeLog.info(name + " : Not documented.");
			} else if (docs.length == 1) {
				JakeLog.info(name + " : " + docs[0]);
			} else {
				final String intro = name + " : ";
				JakeLog.info(intro + docs[0]);
				final String margin = JakeUtilsString.repeat(" ", intro.length());
				for (int i = 1; i < docs.length; i++) {
					JakeLog.info(margin + docs[i]);
				}
			}
		}

		public static void log(List<CommandDescription> actions) {
			Class<?> currentDecClass = null;
			Collections.sort(actions);
			for(final CommandDescription actionDescription : actions) {
				if (actionDescription.declaringClass != currentDecClass) {
					JakeLog.info("From " + actionDescription.declaringClass.getName());
					currentDecClass = actionDescription.declaringClass;
				}
				JakeLog.shift(1);
				actionDescription.log();
				JakeLog.shift(-1);
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
