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

package dev.jeka.core.tool.builtins.scaffold;

import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.tool.JkDoc;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * Provides method to generate a project skeleton (folder structure, configuration files, ....)
 */
@JkDoc("Generates project skeletons (folder structure and basic build files).")
public class JkScaffoldOptions {

    @JkDoc("Set a specific jeka.version to include in jeka.properties. " +
            "UNDEFINE: no jeka.version specified, " +
            "LATEST: last version found in Maven Central.")
    private String jekaVersion;

    @JkDoc("Set a specific jeka.distrib.location to include in jeka.properties.")
    private String jekaLocation;

    @JkDoc("Set a specific jeka.distrib.repo to include in jeka.properties.")
    private String jekaDistribRepo;

    @JkDoc("Coma separated string representing properties to add to jeka.properties.")
    private String extraJekaProps = "";

    @JkDoc(
            "Set the path of a file containing the exact content of the jeka.properties file to generate.%n"
        +   "If this field is set, all others related to jeka.properties generation are ignored.")
    private Path rawJekaPropsContentPath;

    public void applyTo(JkScaffold scaffold) {

        // add extra content to jeka.properties

        if (rawJekaPropsContentPath != null) {
            scaffold.setRawJekaPropsPath(rawJekaPropsContentPath);
        } else if (extraJekaProps != null) {
            Arrays.stream(extraJekaProps.split(",")).forEach(scaffold::addJekaPropValue);
            scaffold
                    .setJekaVersion(jekaVersion)
                    .setJekaDistribLocation(jekaLocation)
                    .setJekaDistribRepo(jekaDistribRepo);
        }

    }


}
