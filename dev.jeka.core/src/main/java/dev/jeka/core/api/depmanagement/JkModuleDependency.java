package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;

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

    private final JkModuleId moduleId;

    private final JkVersion version;

    private final JkTransitivity transitivity;

    private final List<JkDependencyExclusion> exclusions;

    private final Set<JkArtifactSpecification> artifactSpecifications;

    private final Path ideProjectDir;

    private JkModuleDependency(JkModuleId moduleId, JkVersion version,
                               JkTransitivity transitivity, List<JkDependencyExclusion> exclusions,
                               Set<JkArtifactSpecification> artifactSpecifications, Path ideProjectDir) {
        JkUtilsAssert.argument(moduleId != null, "module cannot be null.");
        JkUtilsAssert.argument(version != null, moduleId + " version cannot be null.");
        JkUtilsAssert.argument(exclusions != null, moduleId + " module dependency can't be instantiated with null excludes, use empty list instead");
        this.moduleId = moduleId;
        this.version = version;
        this.transitivity = transitivity;
        this.exclusions = exclusions;
        this.artifactSpecifications = artifactSpecifications;
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
        return new JkModuleDependency(moduleId, version, null, Collections.emptyList(), Collections.emptySet(), null);
    }

    /**
     * Creates a {@link JkModuleDependency} to the specified versioned module.
     */
    public static JkModuleDependency of(JkVersionedModule versionedModule) {
        return of(versionedModule.getModuleId(), versionedModule.getVersion());
    }

    /**
     * Creates a {@link JkModuleDependency} to its getModuleId and
     * <code>JkVersionrange</code>.
     */
    @SuppressWarnings("unchecked")
    public static JkModuleDependency of(JkModuleId moduleId, String versionRange) {
        return of(moduleId.withVersion(versionRange));
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
     * group:name:classifier:version
     * group:name:classifier:type:version
     *
     * Version can be a '?' if it is unspecified.
     */
    public static JkModuleDependency of(String description) {
        final String[] strings = description.split( ":");
        final String errorMessage = "Dependency specification '" + description + "' is not correct. Should be one of group:name\n" +
                ", group:name:version, 'group:name:classifier:version, 'group:name:classifier:type:version'.\n" +
                "'?' can be used in place of 'version' if this one is unspecified.";
        JkUtilsAssert.argument(isModuleDependencyDescription(description), errorMessage);
        final JkModuleId moduleId = JkModuleId.of(strings[0], strings[1]);
        if (strings.length == 2) {
            return of(moduleId, JkVersion.UNSPECIFIED);
        }
        if (strings.length == 3) {
            return of(moduleId, JkVersion.of(strings[2]));
        } if (strings.length == 4) {
            return of(moduleId, JkVersion.of(strings[3])).withClassifier(strings[2]);
        }
        return of(moduleId, JkVersion.of(strings[4])).withClassifierAndType(strings[2], strings[3]);
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
        return moduleId;
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
        return new JkModuleDependency(moduleId, version, transitivity, exclusions, artifactSpecifications, ideProjectDir);
    }

    /**
     * Returns <code>true</code> if the version of the module for this dependency is not specified.
     */
    public boolean hasUnspecifiedVersion() {
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
        return new JkModuleDependency(moduleId, version, transitivity, exclusions, artifactSpecifications, ideProjectDir);
    }

    /**
     * @see #withClassifierAndType(String, String)
     */
    public JkModuleDependency withClassifier(String classifier) {
        return withClassifierAndType(classifier, null);
    }

    /**
     * Returns a JkModuleDependency identical to this one but with the specified
     * classifier and type as the only {@link JkArtifactSpecification} for this dependency
     */
    public JkModuleDependency withClassifierAndType(String classifier, String type) {
        JkArtifactSpecification artifactSpecification = new JkArtifactSpecification(classifier, type);
        Set<JkArtifactSpecification> artifactSpecifications = Collections.singleton(artifactSpecification);
        return new JkModuleDependency(moduleId, version, transitivity, exclusions, artifactSpecifications, ideProjectDir);
    }

    /**
     * @see #andClassifierAndType(String, String)
     */
    public JkModuleDependency andClassifier(String classifier) {
        return andClassifierAndType(classifier, null);
    }

    /**
     * Returns a JkModuleDependency identical to this one but adding the specified
     * classifier and type {@link JkArtifactSpecification}.
     */
    public JkModuleDependency andClassifierAndType(String classifier, String type) {
        JkArtifactSpecification artifactSpecification = new JkArtifactSpecification(classifier, type);
        Set<JkArtifactSpecification> set = new LinkedHashSet<>(this.artifactSpecifications);
        if (set.isEmpty()) {
            set.add(JkArtifactSpecification.MAIN);
        }
        set.add(artifactSpecification);
        return new JkModuleDependency(moduleId, version, transitivity, exclusions, Collections.unmodifiableSet(set),
                ideProjectDir);
    }

    /**
     * Returns the {@link JkArtifactSpecification}s for this module dependency. It can e empty if no
     * artifact specification as een set. In this case, only the main artifact is taken in account.
     */
    public Set<JkArtifactSpecification> getArtifactSpecifications() {
        return this.artifactSpecifications;
    }

    /**
     * Returns a JkModuleDependency identical to this one but adding the
     * specified exclusion.
     */
    public JkModuleDependency andExclusion(JkDependencyExclusion... depExcludes) {
        return andExclusion(Arrays.asList(depExcludes));
    }

    /**
     * Returns a JkModuleDependency identical to this one but adding the
     * specified exclusion.
     */
    public JkModuleDependency andExclusion(String groupAndName) {
        return andExclusion(JkDependencyExclusion.of(groupAndName));
    }

    /**
     * Returns a JkModuleDependency identical to this one but adding the
     * specified exclusion.
     */
    public JkModuleDependency andExclusion(Iterable<JkDependencyExclusion> depExcludes) {
        final List<JkDependencyExclusion> list = new LinkedList<>(exclusions);
        list.addAll(JkUtilsIterable.listOf(depExcludes));
        return new JkModuleDependency(moduleId, version, transitivity,
                Collections.unmodifiableList(list), artifactSpecifications, ideProjectDir);
    }

    /**
     * Returns modules to exclude to the transitive chain.
     */
    public List<JkDependencyExclusion> getExclusions() {
        return exclusions;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(moduleId + ":" + version);
        artifactSpecifications.forEach(spec ->
                result.append("(classifier=" + spec.classifier + ", type=" + spec.type + ")"));
        return result.toString();
    }

    @Override
    public Path getIdeProjectDir() {
        return ideProjectDir;
    }

    @Override
    public JkModuleDependency withIdeProjectDir(Path path) {
        return new JkModuleDependency(moduleId, version, transitivity, exclusions, artifactSpecifications, path);
    }

    public JkVersionedModule toVersionedModule() {
        return JkVersionedModule.of(moduleId, version);
    }


    private static class NameComparator implements Comparator<JkModuleDependency> {

        @Override
        public int compare(JkModuleDependency o1, JkModuleDependency o2) {
            return JkModuleId.GROUP_NAME_COMPARATOR.compare(o1.getModuleId(), o2.getModuleId());
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JkModuleDependency that = (JkModuleDependency) o;
        if (!moduleId.equals(that.moduleId)) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;
        if (transitivity != that.transitivity) return false;
        if (!exclusions.equals(that.exclusions)) return false;
        if (!artifactSpecifications.equals(that.artifactSpecifications)) return false;
        return ideProjectDir != null ? ideProjectDir.equals(that.ideProjectDir) : that.ideProjectDir == null;
    }

    @Override
    public int hashCode() {
        int result = moduleId.hashCode();
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (transitivity != null ? transitivity.hashCode() : 0);
        result = 31 * result + exclusions.hashCode();
        result = 31 * result + artifactSpecifications.hashCode();
        result = 31 * result + (ideProjectDir != null ? ideProjectDir.hashCode() : 0);
        return result;
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

    /**
     * When declaring a module dependency, we implicitly request for the main artifact of this module. Nevertheless,
     * we can request for getting others artifacts in place or additionally of the main one.
     * This class aims at specifying which artifact are we interested for the dependency.
     */
    public static class JkArtifactSpecification {

        /** Stands for the main artifact */
        public static final JkArtifactSpecification MAIN = new JkArtifactSpecification(null, null);

        private final String classifier;

        private final String type;

        private JkArtifactSpecification (String classifier, String type) {
            this.classifier = classifier;
            this.type = type;
        }

        public String getClassifier() {
            return classifier;
        }

        public String getType() {
            return type;
        }
    }

    @Override
    public boolean matches(JkDependency other) {
        if (other instanceof JkModuleDependency) {
            JkModuleDependency moduleDependency = (JkModuleDependency) other;
            return this.moduleId.equals(moduleDependency.moduleId);
        }
        return false;
    }
}