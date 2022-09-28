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
public final class JkCoordinateDependency implements JkFileDependency.JkTransitivityDependency {

    private final JkCoordinate coordinate;

    private final JkTransitivity transitivity;

    private final List<JkDependencyExclusion> exclusions;

    private final Path ideProjectDir;

    private JkCoordinateDependency(JkCoordinate coordinate,
                                   JkTransitivity transitivity, List<JkDependencyExclusion> exclusions,
                                   Path ideProjectDir) {
        JkUtilsAssert.argument(coordinate != null, "coordinate cannot be null.");
        JkUtilsAssert.argument(exclusions != null, exclusions + " module dependency can't be instantiated with null excludes, use empty list instead");
        this.coordinate = coordinate;
        this.transitivity = transitivity;
        this.exclusions = exclusions;
        this.ideProjectDir = ideProjectDir;
    }

    /**
     * Creates a {@link JkCoordinateDependency} to the specified {@link JkCoordinate}
     */
    @SuppressWarnings("unchecked")
    public static JkCoordinateDependency of(JkCoordinate coordinate) {
        return new JkCoordinateDependency(coordinate, null, Collections.emptyList(),  null);
    }

    public static JkCoordinateDependency of(JkCoordinate.GroupAndName groupAndName) {
        return of(groupAndName.toCoordinate(JkVersion.UNSPECIFIED));
    }

    public static JkCoordinateDependency of(String groupAndName, String version) {
        return of(JkCoordinate.GroupAndName.of(groupAndName).toCoordinate(version));
    }

    public static JkCoordinateDependency of(String description) {
        return of(JkCoordinate.of(description));
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
    public JkCoordinate getCoordinate() {
        return coordinate;
    }

    /**
     * Returns a {@link JkCoordinateDependency} identical to this one but with the specified 'transitive' property.
     */
    public JkCoordinateDependency withTransitivity(JkTransitivity transitivity) {
        return new JkCoordinateDependency(coordinate, transitivity, exclusions, ideProjectDir);
    }


    /**
     * Returns a JkModuleDependency identical to this one but adding the
     * specified exclusion.
     */
    public JkCoordinateDependency andExclusions(JkDependencyExclusion... depExcludes) {
        return andExclusions(Arrays.asList(depExcludes));
    }

    /**
     * Returns a JkModuleDependency identical to this one but adding the
     * specified exclusion.
     */
    public JkCoordinateDependency andExclusion(String groupAndName) {
        return andExclusions(JkDependencyExclusion.of(groupAndName));
    }

    /**
     * Returns a JkModuleDependency identical to this one but adding the
     * specified exclusion.
     */
    public JkCoordinateDependency andExclusions(Iterable<JkDependencyExclusion> depExcludes) {
        final List<JkDependencyExclusion> list = new LinkedList<>(exclusions);
        list.addAll(JkUtilsIterable.listOf(depExcludes));
        return new JkCoordinateDependency(this.coordinate, transitivity,
                Collections.unmodifiableList(list), ideProjectDir);
    }

    /**
     * Returns modules to exclude to the transitive chain.
     */
    public List<JkDependencyExclusion> getExclusions() {
        return exclusions;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(coordinate.toString());
        if (transitivity != null) {
            result.append(" transitivity:" + transitivity);
        }
        return result.toString();
    }

    @Override
    public Path getIdeProjectDir() {
        return ideProjectDir;
    }

    @Override
    public JkCoordinateDependency withIdeProjectDir(Path path) {
        return new JkCoordinateDependency(coordinate, transitivity, exclusions, path);
    }

    public JkCoordinateDependency withVersion(JkVersion version) {
        return new JkCoordinateDependency(coordinate.withVersion(version), transitivity, exclusions, ideProjectDir);
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


    @Override
    public boolean matches(JkDependency other) {
        if (other instanceof JkCoordinateDependency) {
            JkCoordinateDependency moduleDependency = (JkCoordinateDependency) other;
            return this.coordinate.getGroupAndName().equals(moduleDependency.coordinate.getGroupAndName());
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JkCoordinateDependency that = (JkCoordinateDependency) o;

        if (!coordinate.equals(that.coordinate)) return false;
        if (transitivity != null ? !transitivity.equals(that.transitivity) : that.transitivity != null) return false;
        if (!exclusions.equals(that.exclusions)) return false;
        if (ideProjectDir != null ? !ideProjectDir.equals(that.ideProjectDir) : that.ideProjectDir != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = coordinate.hashCode();
        result = 31 * result + (transitivity != null ? transitivity.hashCode() : 0);
        result = 31 * result + exclusions.hashCode();
        result = 31 * result + (ideProjectDir != null ? ideProjectDir.hashCode() : 0);
        return result;
    }




}