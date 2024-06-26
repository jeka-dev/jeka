/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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

    public JkCoordinateDependency getCoordinateDependency() {
        return (JkCoordinateDependency) dependency;
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
        String qualifierName = qualifier == null ? "" :  "[" + qualifier + "] ";
        return qualifierName + dependency;
    }

    public JkQualifiedDependency withQualifier(String qualifier) {
        return of(qualifier, this.dependency);
    }
}
