package org.jake.depmanagement;

import org.jake.utils.JakeUtilsString;

/**
 * A dependency on an external module. External modules are supposed to be located in a repository.
 * The version range identify which versions are likely to be compatible with the project to build.<br/>
 * For example, <code>org.hibernate:hibernate-core:3.0.+</code> is a legal description for an external module dependency.
 * 
 * @author Djeang
 */
public class JakeExternalModule extends JakeDependency {

	public static JakeExternalModule of(JakeModuleId moduleId, JakeVersionRange versionRange) {
		return new JakeExternalModule(moduleId, versionRange, null);
	}

	public static JakeExternalModule of(String group, String name, String version) {
		return of (JakeModuleId.of(group, name), JakeVersionRange.of(version));
	}


	public static JakeExternalModule of(String description) {
		final String[] strings = JakeUtilsString.split(description, ":");
		if (strings.length != 3) {
			throw new IllegalArgumentException("Module should be formated as 'groupName:moduleName:version'. Was " + description);
		}
		return of(strings[0], strings[1], strings[2]);
	}

	private final JakeModuleId module;
	private final JakeVersionRange versionRange;
	private final String classifier;

	private JakeExternalModule(JakeModuleId module, JakeVersionRange versionRange, String classifier) {
		this.module = module;
		this.versionRange = versionRange;
		this.classifier = classifier;
	}



	public JakeModuleId moduleId() {
		return module;
	}

	public JakeVersionRange versionRange() {
		return versionRange;
	}

	/**
	 * Returns the Maven classifier for this dependency. This has only an effect if the dependency
	 * is resolved in a Maven repository.
	 */
	public String mavenClassifier() {
		return classifier;
	}

	public JakeExternalModule withMavenClassifier(String classifier) {
		return new JakeExternalModule(this.module, this.versionRange, classifier);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "=" + module + ":" + versionRange;
	}

}