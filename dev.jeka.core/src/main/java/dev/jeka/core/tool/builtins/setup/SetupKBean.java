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

package dev.jeka.core.tool.builtins.setup;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;

import java.awt.*;
import java.io.IOException;

@JkDoc("Provides convenient methods to perform global configuration tasks as editing global.properties file " +
        "or updating embedded jeka boot scripts.")
public class SetupKBean extends KBean {

    private static final PropFile GLOBAL_PROP_FILE = new PropFile(JkLocator.getGlobalPropertiesFile());

    @JkDoc("Argument for 'addGlobalProp' and 'installApp' methods.")
    public String content;

    @JkDoc("Argument for the 'removeApp' method.")
    public String name;

    @JkDoc("Opens a file explorer window in the JeKA user home directory.")
    public void openHomeDir() throws IOException {
        Desktop.getDesktop().open(JkLocator.getJekaUserHomeDir().toFile());
    }

    @JkDoc("Edits the global.properties file.")
    public void editGlobalProps() throws IOException {
        JkPathFile.of(GLOBAL_PROP_FILE.path).createIfNotExist();
        if (!GraphicsEnvironment.isHeadless()) {
            if (JkUtilsSystem.IS_WINDOWS) {
                JkProcess.of("notepad", GLOBAL_PROP_FILE.path.toString()).exec();
            } else {
                Desktop.getDesktop().edit(GLOBAL_PROP_FILE.path.toFile());
            }
        } else if (!JkUtilsSystem.IS_WINDOWS) {
            JkProcess.of("nano", GLOBAL_PROP_FILE.path.toString())
                    .setInheritIO(true)
                    .exec();
        }
    }

    @JkDoc("Adds a shorthand to the global properties file.\n" +
            "Provide it as 'content=[shorthand-name]=[shorthand content]\n" +
            "E.g. 'jeka operations: addShorthand content=build=project: pack sonarqube: run'.")
    public void addShorthand() {
        if (JkUtilsString.isBlank(content) || !content.contains("=")) {
            JkLog.info("You must specify the shorthand using 'content=[shorthand-name]=[shorthand content].");
        } else {
            String name = JkUtilsString.substringBeforeFirst(content, "=").trim();
            String value = JkUtilsString.substringAfterFirst(content, "=").trim();
            String propName = "jeka.cmd." + name;
            GLOBAL_PROP_FILE.insertProp(propName, value);
        }
    }

    @JkDoc("Creates or updates jeka.ps1 and jeka bash scripts in the current directory.\n" +
            "Uses the running JeKa version to set the script version.")
    public void updateLocalScripts() {
        JkScaffold.createShellScripts(getBaseDir());
    }

}
