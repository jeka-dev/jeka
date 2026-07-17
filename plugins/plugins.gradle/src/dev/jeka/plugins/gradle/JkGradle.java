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

package dev.jeka.plugins.gradle;

import dev.jeka.core.api.system.JkAbstractProcess;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Convenient class wrapping Gradle process.
 */
public final class JkGradle extends JkAbstractProcess<JkGradle> {

    public static final String VERBOSE_ARG = "--debug";

    public static final String STACKTRACE_ARG = "--stacktrace";

    private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

    /**
     * Path to the user-level Gradle directory containing caches and configuration.
     */
    public static final Path USER_GRADLE_DIR = USER_HOME.resolve(".gradle");

    private JkGradle(Path workingDir) {
        super();
    }

    private JkGradle(JkGradle other) {
        super(other);
    }

    /**
     * Creates a Gradle command wrapper for the given working directory.
     * Prefers the Gradle wrapper script if present.
     */
    public static JkGradle of(Path workingDir) {
        return new JkGradle(workingDir).setWorkingDir(workingDir).addParams(gradleCmd(workingDir));
    }

    @Override
    protected JkGradle copy() {
        return new JkGradle(this);
    }

    private static String gradleCmd(Path workingDir) {
        if (JkUtilsSystem.IS_WINDOWS) {
            if (Files.exists(workingDir.resolve("gradlew.bat"))) {
                return "gradlew.bat";
            }
            if (exist("gradle")) {
                return "gradle";
            }
            return "gradle";
        }
        if (Files.exists(workingDir.resolve("gradlew"))) {
            return "./gradlew";
        }
        if (exist("gradle")) {
            return "gradle";
        }
        return "gradle";
    }

    private static boolean exist(String cmd) {
        try {
            return JkProcess.of(cmd, "--version").exec().getExitCode() == 0;
        } catch (final Exception e) {  //NOSONAR
            JkLog.verbose("Error while executing command '%s' : %s", cmd, e.getMessage());
            return false;
        }
    }

}
