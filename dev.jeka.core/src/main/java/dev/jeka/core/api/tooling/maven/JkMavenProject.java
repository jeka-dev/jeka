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

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static dev.jeka.core.api.depmanagement.JkQualifiedDependencySet.*;

/**
 * Represents a Maven project with various utility methods for performing common tasks.
 */
public class JkMavenProject {

    public final Path baseDir;

    private JkMavenProject(Path baseDir) {
        JkUtilsAssert.state(Files.exists(baseDir.resolve("pom.xml")), "No pom.xml file found at %s . Can't process.",
                baseDir);
        this.baseDir = baseDir;
    }

    public static JkMavenProject of(Path baseDir) {
        return new JkMavenProject(baseDir);
    }

    /**
     * Returns a new JkMvn instance to perform a <i>clean package</i>.
     */
    public JkMvn cleanPackage() {
        return mvn().addParams("clean", "package");
    }

    /**
     * Returns a new JkMvn instance to perform a <i>clean install</i>.
     */
    public JkMvn cleanInstall() {
        return mvn().addParams("clean", "install");
    }

    /**
     * Returns the dependencies of this Maven project
     */
    public JkQualifiedDependencySet readDependencies() {
        final Path file = JkUtilsPath.createTempFile("dependency", ".txt");
        mvn().addParams("dependency:list", "-DoutputFile=" + file).exec();
        final JkQualifiedDependencySet result = fromMvnFlatFile(file);
        JkUtilsPath.deleteFile(file);
        return result;
    }

    /**
     * Returns the project's dependencies as JeKa code.
     *
     * @param codeIndent The number of spaces for each indentation level in the generated code.
     */
    public String getDependencyAsJeKaCode(int codeIndent)  {
        StringBuilder sb = new StringBuilder();
        Path effectivePom = JkUtilsPath.createTempFile("jeka-effective-pom-", ".pom");
        mvn().addParamsAsCmdLine("help:effective-pom -Doutput=%s", effectivePom)
                .setLogCommand(JkLog.isVerbose())
                .setLogWithJekaDecorator(JkLog.isVerbose())
                .exec();
        //Path pomPath = getBaseDir().resolve("pom.xml");
        //JkUtilsAssert.state(Files.exists(pomPath), "No pom file found at " + pomPath);
        JkPom pom = JkPom.of(effectivePom);
        sb.append("Compile\n");
        sb.append(JkDependencySet.toJavaCode(codeIndent, pom.getDependencies().getDependenciesHavingQualifier(null,
                COMPILE_SCOPE, PROVIDED_SCOPE), true));

        sb.append("Runtime\n");
        sb.append(JkDependencySet.toJavaCode(codeIndent, pom.getDependencies().getDependenciesHavingQualifier(
                RUNTIME_SCOPE), true));
        sb.append(JkDependencySet.toJavaCode(codeIndent, pom.getDependencies().getDependenciesHavingQualifier(
                PROVIDED_SCOPE), false));

        sb.append("Test\n");
        sb.append(JkDependencySet.toJavaCode(codeIndent, pom.getDependencies().getDependenciesHavingQualifier(
                TEST_SCOPE), true));
        return sb.toString();
    }

    /**
     * Returns the dependencies of this Maven project as a formatted string suitable to be copy/pasted
     * in <i>jeka/project-dependencies.txt</i> file.
     */
    public String getDependenciesAsTxt()  {
        StringBuilder sb = new StringBuilder();
        Path effectivePom = JkUtilsPath.createTempFile("jeka-effective-pom-", ".pom");
        mvn().addParamsAsCmdLine("help:effective-pom -Doutput=%s", effectivePom)
                .setLogCommand(JkLog.isVerbose())
                .setLogWithJekaDecorator(JkLog.isVerbose())
                .exec();
        JkPom pom = JkPom.of(effectivePom);
        sb.append("\n==== COMPILE ====\n");
        sb.append(JkDependencySet.toTxt( pom.getDependencies().getDependenciesHavingQualifier(null,
                COMPILE_SCOPE, PROVIDED_SCOPE), false));

        sb.append("\n\n==== RUNTIME ====\n");
        sb.append(JkDependencySet.toTxt(pom.getDependencies().getDependenciesHavingQualifier(
                RUNTIME_SCOPE), false));
        sb.append(JkDependencySet.toTxt(pom.getDependencies().getDependenciesHavingQualifier(
                PROVIDED_SCOPE), true));

        sb.append("\n\n==== TEST ====\n");
        sb.append(JkDependencySet.toTxt(pom.getDependencies().getDependenciesHavingQualifier(
                TEST_SCOPE), false));

        return sb.toString();
    }

    /**
     * Returns a new instance of {@link JkMvn} to perform Maven commands on this project.
     */
    public JkMvn mvn() {
        return JkMvn.of(baseDir);
    }

    /*
     * Creates a {@link JkDependencySet} from file describing dependencies the following way :
     * <pre>
     * <code>
     * org.springframework:spring-aop:jar:4.2.3.BUILD-SNAPSHOT:compile
     * org.yaml:snakeyaml:jar:1.16:runtime
     * org.slf4j:log4j-over-slf4j:jar:1.7.12:compile
     * org.springframework.boot:spring-boot:jar:1.3.0.BUILD-SNAPSHOT:compile
     * org.hamcrest:hamcrest-core:jar:1.3:test
     * aopalliance:aopalliance:jar:1.0:compile
     * org.springframework:spring-test:jar:4.2.3.BUILD-SNAPSHOT:test
     * org.springframework.boot:spring-boot-autoconfigure:jar:1.3.0.BUILD-SNAPSHOT:compile
     * ch.qos.logback:logback-core:jar:1.1.3:compile
     * org.hamcrest:hamcrest-library:jar:1.3:test
     * junit:junit:jar:4.12:test
     * org.slf4j:slf4j-api:jar:1.7.12:compile
     * </code>
     * </pre>
     *
     * The following format are accepted for each line :
     * <ul>
     * <li>group:name:classifier:version:scope (classifier "jar" equals to no
     * classifier)</li>
     * <li>group:name:version:scope (no classifier)</li>
     * <li>group:name:version (default version is scope)</li>
     * </ul>
     *
     */
    private static JkQualifiedDependencySet fromMvnFlatFile(Path flatFile) {
        List<JkQualifiedDependency> result = new LinkedList<>();
        for (final String line : JkUtilsPath.readAllLines(flatFile)) {
            JkQualifiedDependency scopedDependency = mvnDep(line);
            if (scopedDependency != null) {
                result.add(scopedDependency);
            }
        }
        return JkQualifiedDependencySet.of(result);
    }

    private static JkQualifiedDependency mvnDep(String description) {
        final String[] items = description.trim().split(":");
        if (items.length == 5) {
            final String classifier = items[2];
            final String scope = JkPom.toScope(items[4]);
            JkCoordinate coordinate = JkCoordinate.of(items[0], items[1], items[3]);
            if (!"jar".equals(classifier)) {
                coordinate = coordinate.withClassifiers(classifier);
            }
            JkCoordinateDependency dependency = JkCoordinateDependency.of(coordinate);
            return JkQualifiedDependency.of(scope, dependency);
        }
        final JkCoordinate coordinate = JkCoordinate.of(items[0], items[1], items[2]);
        final JkCoordinateDependency dependency = JkCoordinateDependency.of(coordinate);
        if (items.length == 4) {
            final String scope = JkPom.toScope(items[3]);
            return JkQualifiedDependency.of(scope, dependency);
        }
        if (items.length == 3) {
            return JkQualifiedDependency.of(null, dependency);
        }
        return null;
    }

}
