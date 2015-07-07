package org.jerkar.api.depmanagement;

import org.jerkar.api.utils.JkUtilsString;

/**
 * A dependency on an external module. External modules are supposed to be located in a repository.
 * The version range identify which versions are likely to be compatible with the project to build.<br/>
 * For example, <code>org.hibernate:hibernate-core:3.0.+</code> is a legal description for an external module dependency.
 * 
 * @author Djeang
 */
public class JkExternalModuleDependency extends JkDependency {

	private static final long serialVersionUID = 1L;

	public static boolean isGroupNameAndVersion(String candidate) {
		return JkUtilsString.countOccurence(candidate, ':') == 2;
	}

	public static JkExternalModuleDependency of(JkModuleId moduleId, JkVersionRange versionRange) {
		return new JkExternalModuleDependency(moduleId, versionRange, null, true);
	}

	public static JkExternalModuleDependency of(String group, String name, String version) {
		return of (JkModuleId.of(group, name), JkVersionRange.of(version));
	}


	public static JkExternalModuleDependency of(String description) {
		final String[] strings = JkUtilsString.split(description, ":");
		if (strings.length != 3) {
			throw new IllegalArgumentException("Module should be formated as 'groupName:moduleName:version'. Was " + description);
		}
		return of(strings[0], strings[1], strings[2]);
	}

	private final JkModuleId module;
	private final JkVersionRange versionRange;
	private final String classifier;
	private final boolean transitive;

	private JkExternalModuleDependency(JkModuleId module, JkVersionRange versionRange, String classifier, boolean transitive) {
		this.module = module;
		this.versionRange = versionRange;
		this.classifier = classifier;
		this.transitive = transitive;
	}

	public boolean transitive() {
		return transitive;
	}



	public JkModuleId moduleId() {
		return module;
	}

	public JkVersionRange versionRange() {
		return versionRange;
	}

	public JkExternalModuleDependency transitive(boolean transitive) {
		return new JkExternalModuleDependency(module, versionRange, classifier, transitive);
	}

	public JkExternalModuleDependency resolvedTo(JkVersion version) {
		return new JkExternalModuleDependency(module, JkVersionRange.of(version.name()), classifier, transitive);
	}


	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "=" + module + ":" + versionRange;
	}

}