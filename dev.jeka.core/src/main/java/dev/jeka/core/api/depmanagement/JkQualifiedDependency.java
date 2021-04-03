package dev.jeka.core.api.depmanagement;

/**
 * Many tools as Maven, Ivy, Gradle or Intellij qualify dependencies according their purpose and how
 * they should be used for resolution or publication. <p>
 * Maven and Intellij use 'scope' concept for this purpose, while Gradle and Ivy use 'configuration'.
 * This class aims at representing one dependency associated with such a qualifier, in order to
 * help integration with those tools.
 */
public class JkQualifiedDependency {

    // String representation of a 'scope' or 'configuration'
    // Can be null
    private final String qualifier;

    private final JkDependency dependency;

    private JkQualifiedDependency(String qualifier, JkDependency dependency) {
        this.qualifier = qualifier;
        this.dependency = dependency;
    }

    public static JkQualifiedDependency of(String qualifier, JkDependency dependency) {
        return new JkQualifiedDependency(qualifier, dependency);
    }

    public String getQualifier() {
        return qualifier;
    }

    public JkDependency getDependency() {
        return dependency;
    }

    public JkModuleDependency getModuleDependency() {
        return (JkModuleDependency) dependency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JkQualifiedDependency dependency = (JkQualifiedDependency) o;
        if (qualifier != dependency.qualifier) return false;
        return this.dependency.equals(dependency.dependency);
    }

    @Override
    public int hashCode() {
        int result = qualifier != null ? qualifier.hashCode() : 0;
        result = 31 * result + dependency.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "'" + qualifier + "' " + dependency;
    }

    public JkQualifiedDependency withQualifier(String qualifier) {
        return of(qualifier, this.dependency);
    }
}
