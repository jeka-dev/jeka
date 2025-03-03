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

import dev.jeka.core.api.utils.JkUtilsIterable;

import java.util.List;

/**
 * In Maven repositories, modules are published along a pom.xml metadata containing
 * the transitive dependencies of the module. Here, transitive dependencies can be
 * published with only 2 scopes : either 'compile' nor 'runtime'.<p>
 * This enum specifies how a dependency must take in account its transitive ones.
 */
public class  JkTransitivity {

    private final String value;

    private JkTransitivity(String value) {
        this.value = value;
    }

    /**
     * Dependency will be fetched without any transitive dependencies
     */
    public static final JkTransitivity NONE = new JkTransitivity("NONE");

    /**
     * Dependency will be fetched along transitive dependencies declared as 'compile'
     */
    public static final JkTransitivity COMPILE = new JkTransitivity("COMPILE");

    /**
     * Dependency will be fetched along transitive dependencies declared as 'runtime'
     * or 'compile'
     */
    public static final JkTransitivity RUNTIME = new JkTransitivity("RUNTIME");

    private static final List<JkTransitivity> ORDER = JkUtilsIterable.listOf(NONE, COMPILE, RUNTIME);

    public static JkTransitivity ofDeepest(JkTransitivity left, JkTransitivity right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return ORDER.indexOf(left) > ORDER.indexOf(right)? left : right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JkTransitivity that = (JkTransitivity) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
