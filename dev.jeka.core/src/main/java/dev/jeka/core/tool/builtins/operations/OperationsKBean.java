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

package dev.jeka.core.tool.builtins.operations;

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
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@JkDoc("Provides convenient methods to perform admin tasks.")
public class OperationsKBean extends KBean {

    @JkDoc("Argument for method 'addGlobalProp'.")
    public String content;

    @JkDoc("Open a file explorer window on JeKA user home dir.")
    public void openHomeDir() throws IOException {
        Desktop.getDesktop().open(JkLocator.getJekaUserHomeDir().toFile());
    }

    @JkDoc("Edit global.properties file.")
    public void editGlobalProps() throws IOException {
        Path globalProps = JkLocator.getGlobalPropertiesFile();
        JkPathFile.of(globalProps).createIfNotExist();
        if (!GraphicsEnvironment.isHeadless()) {
            if (JkUtilsSystem.IS_WINDOWS) {
                JkProcess.of("notepad", globalProps.toString()).exec();
            } else {
                Desktop.getDesktop().edit(globalProps.toFile());
            }
        } else if (!JkUtilsSystem.IS_WINDOWS) {
            JkProcess.of("nano", globalProps.toString())
                    .setInheritIO(true)
                    .exec();
        }
    }

    @JkDoc("Adds a shorthand to the global properties file. " +
            "Use 'content=[shorthand-name]=[shorthand content]' argument," +
            " as 'jeka operations: addShorthand content=build=project: pack sonarqube: run'." )
    public void addShorthand() {
        Path globalProps = JkLocator.getGlobalPropertiesFile();
        if (JkUtilsString.isBlank(content) || !content.contains("=")) {
            JkLog.info("You must specify the shorthand using 'content=[shorthand-name]=[shorthand content].");
        } else {
            String name = JkUtilsString.substringBeforeFirst(content, "=").trim();
            String value = JkUtilsString.substringAfterFirst(content, "=").trim();
            String propLine = "jeka.cmd." + name + "=" + value;
            JkPathFile.of(globalProps).createIfNotExist().write("\n" + propLine, StandardOpenOption.APPEND);
            JkLog.info("Added " + propLine + " to " + globalProps + ".");
        }

    }

    @JkDoc("Creates or replaces jeka.ps1 and jeka bash script in the current directory .%n" +
            "The running JeKa version is used for defining jeka scripts version to be created.")
    public void updateLocalScripts() {
        JkScaffold.createShellScripts(getBaseDir());
    }


}
