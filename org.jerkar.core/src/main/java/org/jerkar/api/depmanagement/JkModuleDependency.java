package org.jerkar.api.depmanagement;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.system.JkException;
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
public final class JkModuleDependency implements JkDependency {

    private static final long serialVersionUID = 1L;

    private final JkModuleId module;
    private final JkVersion version;
    private final String classifier;
    private final boolean transitive;
    private final String extension;
    private final List<JkDepExclude> excludes;

    private JkModuleDependency(JkModuleId module, JkVersion version, String classifier,
                               boolean transitive, String extension, List<JkDepExclude> excludes) {
        JkUtilsAssert.notNull(module, " module dependency can't be instantiated without module");
        JkUtilsAssert.notNull(version, module + " module dependency can't instantiate without versionRange");
        JkUtilsAssert
                .notNull(excludes, module + " module dependency can't be instantiated with null excludes, use empty list instead");
        this.module = module;
        this.version = version;
        this.classifier = classifier;
        this.transitive = transitive;
        this.extension = extension;
        this.excludes = excludes;
    }

    /**
     * Comparator for {@link JkModuleDependency} sorting dependency by their group then by their name.
     */
    public static final Comparator<JkModuleDependency> GROUP_NAME_COMPARATOR = new NameComparator();

    /**
     * Returns <code>true</code> if the candidate string is a valid module dependency description.
     */
    public static boolean isModuleDependencyDescription(String candidate) {
        final int colonCount = JkUtilsString.countOccurence(candidate, ':');
        return colonCount == 2 || colonCount == 3;
    }

    /**
     * Creates a {@link JkModuleDependency} to the specified getModuleId and
     * <code>JkVersionrange</code>.
     */
    @SuppressWarnings("unchecked")
    public static JkModuleDependency of(JkModuleId moduleId, JkVersion version) {
        return new JkModuleDependency(moduleId, version, null, true, null,
                Collections.EMPTY_LIST);
    }

    /**
     * Creates a {@link JkModuleDependency} to the specified versioned module.
     */
    public static JkModuleDependency of(JkVersionedModule versionedModule) {
        return of(versionedModule.getModuleId(), versionedModule.getVersion().getValue());
    }

    /**
     * Creates a {@link JkModuleDependency} to its getModuleId and
     * <code>JkVersionrange</code>.
     */
    @SuppressWarnings("unchecked")
    public static JkModuleDependency of(JkModuleId moduleId, String versionRange) {
        return new JkModuleDependency(moduleId, JkVersion.of(versionRange), null, true, null,
                Collections.EMPTY_LIST);
    }

    /**
     * Creates a {@link JkModuleDependency} to its group, name and version
     * range. The version range can be any string accepted by
     * {@link JkVersion#of(String)}.
     */
    public static JkModuleDependency of(String group, String name, String version) {
        return of(JkModuleId.of(group, name), JkVersion.of(version));
    }

    /**
     * group:name
     * group:name:version
     * group:name:type:version
     * group:name:type:artifact:version
     *
     * Version can be a '?' if it is unspecified.
     */
    public static JkModuleDependency of(String description) {
        final String[] strings = description.split( ":");
        final String errorMessage = "Dependency specification '" + description + "' is not correct. Should be one of group:name\n" +
        ", group:name:version, 'group:value:type:version, group:of:type:artifact:version";
        if (strings.length < 2) {
            throw new JkException(errorMessage);
        }
        JkModuleId moduleId = JkModuleId.of(strings[0], strings[1]);
        if (strings.length == 2) {
            return JkModuleDependency.of(moduleId, JkVersion.UNSPECIFIED);
        }
        if (strings.length == 3) {
            return JkModuleDependency.of(moduleId, JkVersion.of(strings[2]));
        }
        if (strings.length ==4) {
            return JkModuleDependency.of(moduleId, JkVersion.of(strings[3])).withExt(strings[2]);
        }
        if (strings.length ==5) {
            return JkModuleDependency.of(moduleId, JkVersion.of(strings[4]))
                    .withClassifier(strings[3]).withExt(strings[2]);
        }
        throw new JkException(errorMessage);
    }

    /**
     * Returns <code>true</code> if this dependency should be resolved transitively (returning the dependencies
     * of this dependency recursively).
     */
    public boolean isTransitive() {
        return transitive;
    }

    /**
     * Returns the getModuleId of this dependency.
     */
    public JkModuleId getModuleId() {
        return module;
    }

    /**
     * Returns the version of the module this dependencies is constrained to.
     */
    public JkVersion getVersion() {
        return version;
    }

    /**
     * Returns a {@link JkModuleDependency} identical to this one but with the specified 'transitive' property.
     */
    public JkModuleDependency isTransitive(boolean transitive) {
        return new JkModuleDependency(module, version, classifier, transitive, extension,
                excludes);
    }

    /**
     * Returns <code>true</code> if the version of the module for this dependency is not specified.
     */
    public boolean hasUnspecifedVersion() {
        return this.version.isUnspecified();
    }

    /**
     * Returns a JkModuleDependency identical to this one but with the specified
     * static version. If the specified version is <code>null</code> then returned version is this one.
     *
     */
    public JkModuleDependency withVersion(JkVersion version) {
        if (version == null) {
            return this;
        }
        return new JkModuleDependency(module, version, classifier,
                transitive, extension, excludes);
    }

    /**
     * Returns a JkModuleDependency identical to this one but with the specified
     * classifier. This has meaning only for Maven module.
     */
    public JkModuleDependency withClassifier(String classifier) {
        return new JkModuleDependency(module, version, classifier, transitive, extension,
                excludes);
    }

    /**
     * Returns the classifier for this module dependency or <code>null</code> if
     * the dependency is done on the main artifact.
     */
    public String getClassifier() {
        return this.classifier;
    }

    /**
     * Returns a JkModuleDependency identical to this one but with the specified
     * artifact getExtension.
     */
    public JkModuleDependency withExt(String extension) {
        String ext = JkUtilsString.isBlank(extension) ? null : extension;
        return new JkModuleDependency(module, version, classifier, transitive, ext,
                excludes);
    }

    /**
     * Returns a JkModuleDependency identical to this one but adding the
     * specified exclusion.
     */
    public JkModuleDependency andExclude(JkDepExclude... depExcludes) {
        return andExclude(Arrays.asList(depExcludes));
    }

    /**
     * Returns a JkModuleDependency identical to this one but adding the
     * specified exclusion.
     */
    public JkModuleDependency andExclude(String groupeAndName) {
        return andExclude(JkDepExclude.of(groupeAndName));
    }

    /**
     * Returns a JkModuleDependency identical to this one but adding the
     * specified exclusion.
     */
    public JkModuleDependency andExclude(Iterable<JkDepExclude> depExcludes) {
        final List<JkDepExclude> list = new LinkedList<>(excludes);
        list.addAll(JkUtilsIterable.listOf(depExcludes));
        return new JkModuleDependency(module, version, classifier, transitive, extension,
                Collections.unmodifiableList(list));
    }

    /**
     * Returns the getExtension for this module dependency or <code>null</code> if
     * the dependency is done on the the default getExtension.
     */
    public String getExt() {
        return this.extension;
    }

    /**
     * Returns modules to exclude to the transitive chain.
     */
    public List<JkDepExclude> getExcludes() {
        return excludes;
    }

    @Override
    public String toString() {
        if (classifier == null) {
            return module + ":" + version;
        }
        return module + ":" + version + ":" + classifier;
    }

    private static class NameComparator implements Comparator<JkModuleDependency> {

        @Override
        public int compare(JkModuleDependency o1, JkModuleDependency o2) {
            return JkModuleId.GROUP_NAME_COMPARATOR.compare(o1.getModuleId(), o2.getModuleId());
        }

    }

}