package org.jerkar.api.depmanagement;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

/**
 * A dependency on an external module. External modules are supposed to be
 * located in a repository. The version range identify which versions are likely
 * to be compatible with the project to build.<br/>
 * For example, <code>org.hibernate:hibernate-core:3.0.+</code> is a legal
 * description for an external module dependency.
 * <p/>
 * You can also define exclusions on module dependencies so artifact or entire
 * module won't be catch up by the dependency manager.
 *
 * @author Jerome Angibaud
 */
public final class JkModuleDependency extends JkDependency {

    private static final long serialVersionUID = 1L;

    public static final Comparator<JkModuleDependency> GROUP_NAME_COMPARATOR = new NameComparator();

    public static boolean isModuleDependencyDescription(String candidate) {
	final int colonCount = JkUtilsString.countOccurence(candidate, ':');
	return colonCount == 2 || colonCount == 3;
    }

    /**
     * Creates a {@link JkModuleDependency} from its moduleId and
     * <code>JkVersionrange</code>.
     */
    @SuppressWarnings("unchecked")
    public static JkModuleDependency of(JkModuleId moduleId, JkVersionRange versionRange) {
	return new JkModuleDependency(moduleId, versionRange, null, true, null, Collections.EMPTY_LIST);
    }

    /**
     * Creates a {@link JkModuleDependency} from its group, name and version
     * range. The version range can be any string accepted by
     * {@link JkVersionRange#of(String)}.
     */
    public static JkModuleDependency of(String group, String name, String version) {
	return of(JkModuleId.of(group, name), JkVersionRange.of(version));
    }

    /**
     * Creates a JkModuleDependency from a formatted string description. The
     * expected format are :
     * <ul>
     * <li>groupName:moduleName:version</li>.
     * <li>groupName:moduleName:version:classifier</li>.
     * <li>groupName:moduleName:version@extension</li>.
     * <li>groupName:moduleName:version:classifier@extension</li>.
     * </ul>
     */
    public static JkModuleDependency of(String description) {
	final String ext;
	if (description.contains("@")) {
	    ext = JkUtilsString.substringAfterLast(description, "@");
	} else {
	    ext = null;
	}
	final String[] strings = JkUtilsString.split(description, ":");
	if (strings.length != 3 && strings.length != 4) {
	    throw new IllegalArgumentException(
		    "Module should be formated as 'groupName:moduleName:version' or 'groupName:moduleName:version:classifier'. Was "
			    + description);
	}
	final JkModuleDependency result = of(strings[0], strings[1], strings[2]).ext(ext).transitive(ext == null);
	if (strings.length == 3) {
	    return result;
	}
	final String classifier = strings[3];
	final boolean transitive = classifier == null ? result.transitive : false;
	return result.classifier(strings[3]).transitive(transitive);
    }

    private final JkModuleId module;
    private final JkVersionRange versionRange;
    private final String classifier;
    private final boolean transitive;
    private final String extension;
    private final List<JkDepExclude> excludes;

    private JkModuleDependency(JkModuleId module, JkVersionRange versionRange, String classifier, boolean transitive,
	    String extension, List<JkDepExclude> excludes) {
	JkUtilsAssert.notNull(module, "Can't instantiate without module");
	JkUtilsAssert.notNull(module, "Can't instantiate without versionRange");
	JkUtilsAssert.notNull(module, "Can't instantiate wit null excludes, use empty list instead");
	this.module = module;
	this.versionRange = versionRange;
	this.classifier = classifier;
	this.transitive = transitive;
	this.extension = extension;
	this.excludes = excludes;
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

    public JkModuleDependency transitive(boolean transitive) {
	return new JkModuleDependency(module, versionRange, classifier, transitive, extension, excludes);
    }

    public boolean hasUnspecifedVersion() {
	return this.versionRange.isUnspecified();
    }

    /**
     * Returns a JkModuleDependency identical to this one but with the specified
     * static version.
     */
    public JkModuleDependency resolvedTo(JkVersion version) {
	return new JkModuleDependency(module, JkVersionRange.of(version.name()), classifier, transitive, extension,
		excludes);
    }

    /**
     * Returns a JkModuleDependency identical to this one but with the specified
     * classifier. This has meaning only for Maven module.
     */
    public JkModuleDependency classifier(String classifier) {
	return new JkModuleDependency(module, versionRange, classifier, transitive, extension, excludes);
    }

    /**
     * Returns the classifier for this module dependency or <code>null</code> if
     * the dependency is done on the main artifact.
     */
    public String classifier() {
	return this.classifier;
    }

    /**
     * Returns a JkModuleDependency identical to this one but with the specified
     * artifact extension.
     */
    public JkModuleDependency ext(String extension) {
	return new JkModuleDependency(module, versionRange, classifier, transitive, extension, excludes);
    }

    /**
     * Returns a JkModuleDependency identical to this one but adding the
     * specified exclusion.
     */
    public JkModuleDependency andExclude(JkDepExclude ... depExcludes) {
	return andExclude(Arrays.asList(depExcludes));
    }

    /**
     * Returns a JkModuleDependency identical to this one but adding the
     * specified exclusion.
     */
    public JkModuleDependency andExclude(Iterable<JkDepExclude> depExcludes) {
	final List<JkDepExclude> list = new LinkedList<JkDepExclude>(excludes);
	list.addAll(JkUtilsIterable.listOf(depExcludes));
	return new JkModuleDependency(module, versionRange, classifier, transitive, extension,
		Collections.unmodifiableList(list));
    }

    /**
     * Returns the extension for this module dependency or <code>null</code> if
     * the dependency is done on the the default extension.
     */
    public String ext() {
	return this.extension;
    }

    public List<JkDepExclude> excludes() {
	return excludes;
    }

    @Override
    public String toString() {
	return this.getClass().getSimpleName() + "=" + module + ":" + versionRange;
    }

    private static class NameComparator implements Comparator<JkModuleDependency> {

	@Override
	public int compare(JkModuleDependency o1, JkModuleDependency o2) {
	    return JkModuleId.GROUP_NAME_COMPARATOR.compare(o1.moduleId(), o2.moduleId());
	}


    }

}