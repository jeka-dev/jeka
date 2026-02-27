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

package dev.jeka.core.api.tooling.maven;

import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.system.JkAbstractProcess;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Convenient class wrapping maven process.
 *
 * @author Jerome Angibaud
 */
public final class JkMvn extends JkAbstractProcess<JkMvn> {

    public static final String VERBOSE_ARG = "-X";

    public static final String FORCE_UPDATE_ARG = "-U";

    private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

    /**
     * Path to the local Maven repository dir containing user configuration and local repo.
     */
    public static final Path USER_M2_DIR = USER_HOME.resolve(".m2");

    private JkMvn(Path workingDir) {
        super();
    }

    private JkMvn(JkMvn other) {
        super(other);
    }

    /**
     * Creates a Maven command wrapper.
     */
    public static JkMvn of(Path workingDir) {
        return new JkMvn(workingDir).setWorkingDir(workingDir).addParams(mvnCmd(workingDir));
    }

    /**
     * Returns the path to the local Maven repository.
     */
    public static Path getM2LocalRepo() {
        return USER_M2_DIR.resolve("repository");  // TODO naive implementation : handle settings.xml
    }

    /**
     * Returns the XML document representing the Maven settings file, or null if the file does not exist.
     */
    public static JkDomDocument settingsXmlDoc() {
        Path file = USER_M2_DIR.resolve("settings");
        if (!Files.exists(file)) {
            return null;
        }
        return JkDomDocument.parse(file);
    }

    @Override
    protected JkMvn copy() {
        return new JkMvn(this);
    }

    public JkProcess mvnProcess() {
        return JkProcess.of(mvnCmd(this.getWorkingDir()))
                .setLogCommand(true)
                .setLogWithJekaDecorator(true);
    }

    // TODO handle mvn wrapper
    private static String mvnCmd(Path workingDir) {
        if (JkUtilsSystem.IS_WINDOWS) {
            if (Files.exists(workingDir.resolve("mvnw.cmd"))) {
                return "mvnw.cmd";
            }
            if (exist("mvn.bat")) {
                return "mvn.bat";
            }
            if (exist("mvn.cmd")) {
                return "mvn.cmd";
            }
            return null;
        }

        // non windows
        if (Files.exists(workingDir.resolve("mvnw"))) {
            return "./mvnw";
        }
        if (exist("mvn")) {
            return "mvn";
        }
        return null;
    }

    private static boolean exist(String mvnCmd) {
        String command = mvnCmd + " -version";
        try {
            final int result = Runtime.getRuntime().exec(command).waitFor();
            return result == 0;
        } catch (final Exception e) {  //NOSONAR
            JkLog.verbose("Error while executing command '%s' : %s", command, e.getMessage());
            return false;
        }
    }

}
