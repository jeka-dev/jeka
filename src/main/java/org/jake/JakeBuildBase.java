package org.jake;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.jake.file.JakeDirView;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;

/**
 * Base project builder defining some commons tasks and utilities
 * necessary for any kind of project, regardless involved technologies.
 * 
 * @author Jerome Angibaud
 */
public class JakeBuildBase {

	protected static final File WORKING_DIR = JakeUtilsFile.canonicalFile(new File("."));

	protected JakeBuildBase() {
	}

	protected Class<JakeOptions> optionClass() {
		return JakeOptions.class;
	}

	/**
	 * The default level to use for the default logger when
	 * 'java.util.logging.level' system property is not set.
	 */
	protected Level getDefaultLogLevel() {
		return Level.INFO;
	}

	/**
	 * The current version for this project. Might look like "0.6.3".
	 */
	protected String version() {
		return null;
	}

	/**
	 * The string used to suffix produced artefacts name to indicate version.
	 * Might look like "-0.6.3".
	 */
	protected String versionSuffix() {
		if (version() == null || version().isEmpty()) {
			return "";
		}
		return "-" + version();
	}

	/**
	 * The project name. This is likely to used in produced artefacts.
	 */
	protected String projectName() {
		return JakeUtilsFile.fileName(baseDir().root());
	}

	/**
	 * The base directory for this project. All file/directory path are
	 * resolved from this directory.
	 */
	protected JakeDirView baseDir() {
		return JakeDirView.of(WORKING_DIR);
	}

	protected File baseDir(String relativePath) {
		return JakeDirView.of(WORKING_DIR).file(relativePath);
	}

	/**
	 * The output directory where all the final and intermediate
	 * artefacts are generated.
	 */
	protected JakeDirView buildOuputDir() {
		return baseDir().sub("build/output").createIfNotExist();
	}

	protected File buildOuputDir(String relativePath) {
		return buildOuputDir().file(relativePath);
	}



	// ------------ Operations ------------

	@JakeDoc("Clean the output directory.")
	public void clean() {
		JakeLog.start("Cleaning output directory " + buildOuputDir().root().getPath() );
		JakeUtilsFile.deleteDirContent(buildOuputDir().root());
		JakeLog.done();
	}

	@JakeDoc("Conventional method standing for the default operations to perform.")
	public void doDefault() {
		clean();
	}

	@JakeDoc("Display all available actions defined in this build.")
	public void help() {
		JakeLog.info("Available action(s) for build '" + this.getClass().getName() + "' : " );
		final List<ActionDescription> list = new LinkedList<JakeBuildBase.ActionDescription>();
		for (final Method method : this.getClass().getMethods()) {

			if (!method.getReturnType().equals(void.class)) {
				continue;
			}
			if (method.getParameterTypes().length != 0) {
				continue;
			}
			if (JakeUtilsReflect.isMethodPublicIn(Object.class, method.getName())) {
				continue;
			}
			final int modifier = method.getModifiers();
			if (Modifier.isAbstract(modifier) || Modifier.isStatic(modifier)) {
				continue;
			}
			final JakeDoc jakeDoc = JakeUtilsReflect.getInheritedAnnotation(method, JakeDoc.class);
			final ActionDescription actionDescription;
			if (jakeDoc != null) {
				actionDescription = new ActionDescription(method, jakeDoc.value());
			} else {
				actionDescription = new ActionDescription(method, null);
			}
			list.add(actionDescription);
		}
		ActionDescription.log(list);
	}

	private static class ActionDescription implements Comparable<ActionDescription> {

		private final String name;
		private final String[] docs;
		private final Class<?> declaringClass;

		public ActionDescription(Method method, String[] docs) {
			super();
			this.name = method.getName().equals("doDefault") ? "default" : method.getName();
			this.docs = docs;
			this.declaringClass = method.getDeclaringClass();
		}

		@Override
		public int compareTo(ActionDescription other) {
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

		public static void log(List<ActionDescription> actions) {
			Class currentDecClass = null;
			Collections.sort(actions);
			for(final ActionDescription actionDescription : actions) {
				if (actionDescription.declaringClass != currentDecClass) {
					JakeLog.nextLine();
					JakeLog.info("From " + actionDescription.declaringClass.getName());
					currentDecClass = actionDescription.declaringClass;
				}
				JakeLog.increaseOffset(1);
				actionDescription.log();
				JakeLog.increaseOffset(-1);
			}
			JakeLog.nextLine();
		}


	}



}
