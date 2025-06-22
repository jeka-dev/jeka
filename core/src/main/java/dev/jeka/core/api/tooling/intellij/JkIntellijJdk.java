/*
 * Copyright 2014-2025  the original author or authors.
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

package dev.jeka.core.api.tooling.intellij;

import dev.jeka.core.api.java.JkJavaVersion;

public class JkIntellijJdk {

    private static final String INHERITED = "$INHERITED$";

    private final String jdkName;

    JkIntellijJdk(String jdkName) {
        this.jdkName = jdkName;
    }

    public static JkIntellijJdk ofInherited() {
        return new JkIntellijJdk(INHERITED);
    }

    public static JkIntellijJdk ofName(String jdkName) {
        return new JkIntellijJdk(jdkName);
    }

    /**
     * Creates a {@link JkIntellijJdk} instance that represents a Jeka-managed JDK
     * with the specified distribution and Java version.
     *
     * @param distrib the distribution identifier to include in the JDK name.
     *                If null, only the Java version will be used.
     * @param jdkVersion the Java version for the JDK.
     * @return a {@link JkIntellijJdk} instance with a name in the format "jeka-{distrib}-{jdkVersion}"
     *         if `distrib` is provided, otherwise "jeka-{jdkVersion}".
     */
    public static JkIntellijJdk ofJekaJdk(String distrib, JkJavaVersion jdkVersion) {
        if (distrib == null) {
            return of("jeka-" + jdkVersion);
        }
        return of("jeka-" + distrib + "-" + jdkVersion);
    }

    public static JkIntellijJdk of(String jdkName) {
        return new JkIntellijJdk(jdkName);
    }

    public String getJdkName() {
        return jdkName;
    }

    public boolean isInherited() {
        return INHERITED.equals(getJdkName());
    }

    public boolean isJekaManaged() {
        return jdkName.startsWith("jeka-");
    }

    public JkJavaVersion getJdkVersion() {
        if (isInherited()) {
            return null;
        }
        return Utils.guessFromJProjectJdkName(jdkName);
    }

    @Override
    public String toString() {
        return jdkName;
    }

    public boolean isCustom() {
        return jdkName.chars().filter(ch -> ch == '-').count() == 1;
    }
}
