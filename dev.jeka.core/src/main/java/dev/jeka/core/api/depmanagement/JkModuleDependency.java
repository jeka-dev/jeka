package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.util.*;

/**
 * A dependency on an external module supposed to be located in a binary repository. <p>
 * The version or version range identify which versions are likely to be compatible with the project to build.<br/>
 * For example, both <code>org.hibernate:hibernate-core:3.0.+</code> and
 * <code>org.hibernate:hibernate-core:3.0.1</code> are a legal descriptions for module dependency.
 * <p/>
 *
 * You can also define exclusions on module dependencies so artifact or entire
 * module won't be catch up by the dependency manager.
 *
 * @author Jerome Angibaud
 */
public final class JkModuleDependency implements JkDependency {

    private final JkModuleId module;
    private final JkVersion version;
    private final String classifier;
    private final JkTransitivity transitivity;
    private final String extension;
    private final List<JkDependencyExclusion> excludes;
    private final Path ideProjectDir;

    private JkModuleDependency(JkModuleId module, JkVersion version, String classifier,
                               JkTransitivity transitivity, String extension, List<JkDependencyExclusion> excludes, Path ideProjectDir) {
        JkUtilsAssert.argument(module != null, "module cannot be null.");
        JkUtilsAssert.argument(version != null, module + " version cannot be null.");
        JkUtilsAssert.argument(excludes != null, module + " module dependency can't be instantiated with null excludes, use empty list instead");
        this.module = module;
        this.version = version;
        this.classifier = classifier;
        this.transitivity = transitivity;
        this.extension = extension;
        this.excludes = excludes;
        this.ideProjectDir = ideProjectDir;
    }

    /**
     * Comparator for {@link JkModuleDependency} sorting dependency by their group then by their name.
     */
    public static final Comparator<JkModuleDependency> GROUP_NAME_COMPARATOR = new NameComparator();



    /**
     * Creates a {@link JkModuleDependency} to the specified getModuleId and
     * <code>JkVersionrange</code>.
     */
    @SuppressWarnings("unchecked")
    public static JkModuleDependency of(JkModuleId moduleId, JkVersion version) {
        return new JkModuleDependency(moduleId, version, null, JkTransitivity.RUNTIME, null,
                Collections.EMPTY_LIST, null);
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
        return new JkModuleDependency(moduleId, JkVersion.of(versionRange), null, JkTransitivity.RUNTIME, null,
                Collections.EMPTY_LIST, null);
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
     * Description can be :
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
        JkUtilsAssert.argument(isModuleDependencyDescription(description), errorMessage);
        final JkModuleId moduleId = JkModuleId.of(strings[0], strings[1]);
        if (strings.length == 2) {
            return of(moduleId, JkVersion.UNSPECIFIED);
        }
        if (strings.length == 3) {
            return of(moduleId, JkVersion.of(strings[2]));
        }
        if (strings.length ==4) {
            return of(moduleId, JkVersion.of(strings[3])).withExt(strings[2]);
        }
        else { // length = 5
            return of(moduleId, JkVersion.of(strings[4]))
                    .withClassifier(strings[3]).withExt(strings[2]);
        }
    }

    /**
     * Returns <code>true</code> if the specified candidate matches to a module description.
     * @see #of(String)
     */
    public static boolean isModuleDependencyDescription(String candidate) {
        final String[] strings = candidate.split( ":");
        return strings.length >= 2 && strings.length <= 5;
    }

    /**
     * Returns <code>true</code> if this dependency should be resolved transitively (returning the dependencies
     * of this dependency recursively).
     */
    public JkTransitivity getTransitivity() {
        return transitivity;
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
    public JkModuleDependency withTransitivity(JkTransitivity transitivity) {
        return new JkModuleDependency(module, version, classifier, transitivity, extension,
                excludes, ideProjectDir);
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
                transitivity, extension, excludes, ideProjectDir);
    }

    /**
     * Returns a JkModuleDependency identical to this one but with the specified
     * classifier. This has meaning only for Maven module.
     */
    public JkModuleDependency withClassifier(String classifier) {
        return new JkModuleDependency(module, version, classifier, transitivity, extension, excludes, ideProjectDir);
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
        final String ext = JkUtilsString.isBlank(extension) ? null : extension;
        return new JkModuleDependency(module, version, classifier, transitivity, ext, excludes, ideProjectDir);
    }

    /**
     * Returns a JkModuleDependency identical to this one but adding the
     * specified exclusion.
     */
    public JkModuleDependency andExclude(JkDependencyExclusion... depExcludes) {
        return andExclude(Arrays.asList(depExcludes));
    }

    /**
     * Returns a JkModuleDependency identical to this one but adding the
     * specified exclusion.
     */
    public JkModuleDependency andExclude(String groupeAndName) {
        return andExclude(JkDependencyExclusion.of(groupeAndName));
    }

    /**
     * Returns a JkModuleDependency identical to this one but adding the
     * specified exclusion.
     */
    public JkModuleDependency andExclude(Iterable<JkDependencyExclusion> depExcludes) {
        final List<JkDependencyExclusion> list = new LinkedList<>(excludes);
        list.addAll(JkUtilsIterable.listOf(depExcludes));
        return new JkModuleDependency(module, version, classifier, transitivity, extension,
                Collections.unmodifiableList(list), ideProjectDir);
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
    public List<JkDependencyExclusion> getExclusioins() {
        return excludes;
    }

    @Override
    public String toString() {
        if (classifier == null) {
            return module + ":" + version;
        }
        return module + ":" + version + ":" + classifier;
    }

    @Override
    public Path getIdeProjectDir() {
        return ideProjectDir;
    }

    @Override
    public JkModuleDependency withIdeProjectDir(Path path) {
        return new JkModuleDependency(module, version, classifier, transitivity, extension, excludes, path);
    }

    public JkVersionedModule toVersionedModule() {
        return JkVersionedModule.of(module, version);
    }


    private static class NameComparator implements Comparator<JkModuleDependency> {

        @Override
        public int compare(JkModuleDependency o1, JkModuleDependency o2) {
            return JkModuleId.GROUP_NAME_COMPARATOR.compare(o1.getModuleId(), o2.getModuleId());
        }

    }

    /*
     * The equals method is implemented to consider equals two modules dependencies
     * that may be not the same level of precision. ut at list, they should have
     * the same moduleId in common.<br/>
     * For example 'mygroup:myart:1.0' is considered equals to 'mygroup:myart'.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JkModuleDependency that = (JkModuleDependency) o;
        if (!module.equals(that.module)) return false;
        if (!equalsOrOneIsUnspecified(version, that.version)) return false;
        if (!equalsOrOneIsNull(classifier, that.classifier)) return false;
        if (!equalsOrOneIsNull(extension, that.extension)) return false;
        return equalsOrOneIsNull(excludes, that.excludes);
    }

    @Override
    public int hashCode() {
        return module.hashCode();
    }

    private static boolean equalsOrOneIsNull(Object first, Object second) {
        if (first == null || second == null) {
            return true;
        }
        return first.equals(second);
    }

    private static boolean equalsOrOneIsUnspecified(JkVersion first, JkVersion second) {
        if (first == null || second == null) {
            return true;
        }
        if (first.isUnspecified() || second.isUnspecified()) {
            return true;
        }
        return first.equals(second);
    }
}