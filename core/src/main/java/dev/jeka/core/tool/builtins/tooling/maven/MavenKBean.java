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

package dev.jeka.core.tool.builtins.tooling.maven;

import dev.jeka.core.api.crypto.gpg.JkGpgSigner;
import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.project.JkBuildable;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.maven.JkMavenProject;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkRequire;
import dev.jeka.core.tool.JkRunbase;
import dev.jeka.core.tool.KBean;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@JkDoc("Manages Maven publication for project and 'jeka-src'.")
public final class MavenKBean extends KBean {

    public enum PredefinedRepo {

        /**
         * Represents the OSSRH (Sonatype's Open Source Software Repository Hosting) repository for deploying to
         * Maven central. <br/>
         * This expects the following properties to be presents for granting publish rights :
         * <ul>
         *     <li><code>jeka.repos.publish.username</code> : ossrh user name</li>
         *     <li><code>jeka.repos.publish.password</code> : ossrh password</li>
         *     <li><code>jeka.gpg.secret-key</code> : armored ascii gpg secret key for signing</li>
         *     <li><code>jeka.gpg.passphrase</code> : passphrase for gpg secret</li>
         * </ul>
         *
         * This variable is defined in the PredefinedRepo enum.
         */
        OSSRH
    }

    @JkDoc("Indentation size for 'showPomDeps' output.")
    public int codeIndent = 4;

    public final JkPublicationOptions publication = new JkPublicationOptions();

    private JkMavenPublication mavenPublication;

    private final JkConsumers<JkMavenPublication> mavenPublicationCustomizer = JkConsumers.of();

    @JkDoc("Displays Maven publication information on the console.")
    public void info() {
        JkLog.info(getMavenPublication().info());
    }

    @JkDoc("Publishes the Maven publication on the repositories specified inside this publication.")
    public void publish() {
        getMavenPublication().publish();
    }

    @JkDoc("Publishes the Maven publication on the local JeKa repository.")
    public void publishLocal() {
        getMavenPublication().publishLocal();
    }

    @JkDoc("Publishes the Maven publication on the local M2 repository. This is the local repository of Maven.")
    public void publishLocalM2() {
        getMavenPublication().publishLocalM2();
    }

    @JkDoc("Displays Java code for declaring dependencies based on pom.xml. The pom.xml file is supposed to be in root directory.")
    public void migrateDeps()  {
        System.out.println("───────────────────────────────────────────────────────────────────────────────");
        System.out.println("Java code snippets:");
        System.out.println("───────────────────────────────────────────────────────────────────────────────");
        JkLog.info(JkMavenProject.of(getBaseDir()).getDependencyAsJeKaCode(codeIndent));
        System.out.println("───────────────────────────────────────────────────────────────────────────────");
        System.out.println("dependencies.txt");
        System.out.println("───────────────────────────────────────────────────────────────────────────────");
        JkLog.info(JkMavenProject.of(getBaseDir()).getDependenciesAsTxt());
    }

    @JkRequire
    private static Class<? extends KBean> requireBuildable(JkRunbase runbase) {
        return runbase.getBuildableKBeanClass();
    }

    /**
     * Returns the Maven Publication associated with this KBean
     */
    public JkMavenPublication getMavenPublication() {

        // maven Can't be instantiated in init(), cause it will fail if there is no project or self kbean,
        // that may happen when doing a 'showPomDeps'.
        if (mavenPublication != null) {
            return mavenPublication;
        }
        // Configure with ProjectKBean if present
        JkBuildable buildable = this.getRunbase().getBuildable();
        JkUtilsAssert.state(buildable != null, "No buildable is found for runbase %s for publication.",
                getRunbase().getBaseDir());
        mavenPublication = JkMavenPublication.of(buildable);

        this.publication.metadata.applyTo(mavenPublication);

        // Add Publish Repos from JKProperties
        mavenPublication.setRepos(getPublishReposFromProps());

        this.mavenPublicationCustomizer.accept(mavenPublication);

        // Add artifacts declared in "publication.extraArtifacts"
        publication.extraArtifacts().forEach(mavenPublication::putArtifact);
        
        return mavenPublication;
    }

    /**
     * @see #customizePublication(String, Consumer)
     */
    public void customizePublication(Consumer<JkMavenPublication> publicationCustomizer) {
        this.customizePublication(publicationCustomizer.toString(), publicationCustomizer);
    }

    /**
     * Customizes the Maven publication by appending a custom action to the publication customization process.
     *
     * @param customizationName The name of the customization for identification purposes.
     * @param publicationCustomizer A Consumer that applies custom configurations to the Maven publication.
     */
    public void customizePublication(String customizationName, Consumer<JkMavenPublication> publicationCustomizer) {
        JkUtilsAssert.state(mavenPublication == null, "Maven publication has already been initialized, " +
                "the customization can not be taken in account.");
        this.mavenPublicationCustomizer.append(customizationName, publicationCustomizer);
    }

    /**
     * Customizes the Maven dependencies by applying a modifier function to the dependency set.
     * This allows for dynamic alterations to the dependencies before the Maven publication process.
     */
    public void customizePublishedDeps(Function<JkDependencySet, JkDependencySet> modifier) {
        this.mavenPublicationCustomizer.append(mavenPublication -> {
            mavenPublication.customizeDependencies(modifier);
        });
    }

    private JkRepoSet getPublishReposFromProps() {
        JkRepoProperties repoProperties = JkRepoProperties.of(this.getRunbase().getProperties());
        if (publication.predefinedRepo == PredefinedRepo.OSSRH) {
            JkGpgSigner signer = JkGpgSigner.ofStandardProperties();
            return JkRepoSet.ofOssrhSnapshotAndRelease(repoProperties.getPublishUsername(),
                    repoProperties.getPublishPassword(), signer);
        }
        JkRepoSet result = repoProperties.getPublishRepository();
        if (result.getRepos().isEmpty()) {
            result = result.and(JkRepo.ofLocal());
        }
        return result;
    }



    public static class JkPomMetadata {

        @JkDoc("Human-friendly name for the project to publish")
        public String projectName;

        @JkDoc("Description for the project to publish")
        public String projectDescription;

        @JkDoc("The page to visit to know more about the project")
        public String projectUrl;

        @JkDoc("The url to fetch source code, as the git repo url")
        public String projectScmUrl;

        @JkDoc("Comma separated list of license formated as <license name>:<license url>")
        @JkDepSuggest(versionOnly = true, hint =
                "Apache License V2.0:https://www.apache.org/licenses/LICENSE-2.0.html," +
                "MIT License:https://www.mit.edu/~amini/LICENSE.md")
        public String licenses;

        @JkDoc("Comma separated list of developers formatted as <dev nam>:<dev email>")
        public String developers;


        void applyTo(JkMavenPublication publication) {
            if (projectName != null) {
                publication.pomMetadata.setProjectName(projectName);
            }
            if (projectDescription != null) {
                publication.pomMetadata.setProjectDescription(projectDescription);
            }
            if (projectUrl != null) {
                publication.pomMetadata.setProjectUrl(projectUrl);
            }
            if (projectScmUrl != null) {
                publication.pomMetadata.setScmUrl(projectScmUrl);
            }
            if (licenses != null) {
                Arrays.stream(licenses.split(",")).forEach(
                        item -> {
                            String[] licenseItems = item.split(":");
                            String licenseName = licenseItems[0];
                            String licenseUrl = licenseItems[1];
                            publication.pomMetadata.addLicense(licenseName, licenseUrl);
                        }
                );
            }
            if (developers != null) {
                Arrays.stream(developers.split(",")).forEach(
                        item -> {
                            String[] devItems = item.split(":");
                            String devName = devItems[0];
                            String devEmail = devItems[1];
                            publication.pomMetadata.addDeveloper(devName, devEmail, "", "");
                        }
                );
            }
        }
    }

    public static class JkPublicationOptions {

        @JkDoc("POM metadata to publish. Mainly useful for publishing to Maven Central")
        public final JkPomMetadata metadata = new JkPomMetadata();

        @JkDoc("If not null, the publication will be published on this repo")
        public PredefinedRepo predefinedRepo;

        @JkDoc("Coma separated string of artifact classifiers to publish, in format [classifier] or [classifier].[extension].\n" +
               "This assumes the artifact file be present in jeka-output dir.\n" +
                "Example: 'uber', 'doc.zip'")
        public String extraArtifacts;

        private JkPublicationOptions() {
        }

        private List<JkArtifactId> extraArtifacts() {
            if (JkUtilsString.isBlank(extraArtifacts)) {
                return Collections.emptyList();
            }
            return Arrays.stream(extraArtifacts.split(","))
                    .map(String::trim)
                    .map(JkPublicationOptions::parse)
                    .collect(Collectors.toList());
        }

        private static JkArtifactId parse(String artifactId) {
            if (artifactId.contains(".")) {
                return JkArtifactId.of(JkUtilsString.substringBeforeFirst(artifactId, "."),
                        JkUtilsString.substringAfterFirst(artifactId, "."));
            }
            return JkArtifactId.of(artifactId, "jar");
        }

    }

}
