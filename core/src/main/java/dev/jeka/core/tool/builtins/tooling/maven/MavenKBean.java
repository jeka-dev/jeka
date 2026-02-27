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
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.project.JkBuildable;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.git.JkVersionFromGit;
import dev.jeka.core.api.tooling.maven.JkMavenProject;
import dev.jeka.core.api.tooling.maven.JkMvn;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.git.JkGitVersioning;

import java.io.File;
import java.nio.file.CopyOption;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    @JkDoc("Arguments to pass to mvn command while building. Examples 'clean test', '-Pnative -X'")
    public String args = "";

    @JkDoc("In multi-module projects, the module containing the application to build. " +
            "Defaults to root module if not specified.")
    public String appModule;

    @JkDoc
    private final JkPublicationOptions pub = new JkPublicationOptions();

    private JkMavenPublication publication;

    @JkDoc("Displays Maven publication information on the console.")
    public void info() {
        JkLog.info(getPublication().info());
    }

    @JkDoc("Publishes the Maven publication on the repositories specified inside this publication.")
    public void publish() {
        getPublication().publish();
    }

    @JkDoc("Publishes the Maven publication on the local JeKa repository.")
    public void publishLocal() {
        getPublication().publishLocal();
    }

    @JkDoc("Publishes the Maven publication on the local M2 repository. This is the local repository of Maven.")
    public void publishLocalM2() {
        getPublication().publishLocalM2();
    }

    @JkDoc("Runs Maven from Jeka using arguments from 'args' field.")
    public void wrap() {
        doWrap(JkMavenProject.of(getBaseDir()));
    }

    @JkDoc("Calls Maven 'package' from Jeka and copy produced artifacts to jeka-output.")
    public void wrapPackage() {
        JkMavenProject mvnRootModule = JkMavenProject.of(getBaseDir());
        JkMavenProject mvnAppModule = mvnRootModule;
        if (!JkUtilsString.isBlank(appModule)) {
            mvnAppModule = JkMavenProject.of(getBaseDir().resolve(appModule));
        }
        doWrap(mvnRootModule, "clean", "package", "-Dmaven.test.skip=true");
        String baseFileName = mvnAppModule.getBuildFileName();
        var outputFilesTree = JkPathTree.of(mvnAppModule.getTargetDir())
                .andMatching(baseFileName + "*");
        if (!outputFilesTree.containFiles()) {
            throw new IllegalStateException("No output files found after Maven package execution. " +
                    "Check Maven configuration.");
        }
        if (JkLog.isVerbose()) {
            outputFilesTree.stream().forEach(outputFile ->
                JkLog.verbose("Copying file: " + outputFile)
            );
        }
        outputFilesTree.copyTo(this.getOutputDir(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void doWrap(JkMavenProject mavenProject, String... arguments) {
        String newPath =  JkJavaProcess.CURRENT_JAVA_EXEC_DIR + File.pathSeparator + System.getenv("PATH");
        JkMvn jkMvn = mavenProject.mvn()
                .setEnv("PATH", newPath)
                .setEnv("JAVA_HOME", JkJavaProcess.CURRENT_JAVA_HOME.toString())
                .setEnv("GRAALVM_HOME", JkJavaProcess.CURRENT_JAVA_HOME.toString()) // might use in the case of native compiler
                .addParams(arguments)
                .addParamsAsCmdLine(args);
        jkMvn.exec();
    }

    @JkDoc("Displays Java code for declaring dependencies based on pom.xml. The pom.xml file is supposed to be in root directory.")
    public void migrateDeps()  {
        String separator = "_";
        int repeat = 100;
        System.out.println(JkUtilsString.repeat(separator, repeat));
        System.out.println("Java code snippets:");
        System.out.println(JkUtilsString.repeat(separator, repeat));
        JkLog.info(JkMavenProject.of(getBaseDir()).getDependencyAsJeKaCode(codeIndent));
        System.out.println(JkUtilsString.repeat(separator, repeat));
        System.out.println("dependencies.txt");
        System.out.println(JkUtilsString.repeat(separator, repeat));
        JkLog.info(JkMavenProject.of(getBaseDir()).getDependenciesAsTxt());
    }

    @JkRequire
    private static Class<? extends KBean> requireBuildable(JkRunbase runbase) {
        return runbase.getBuildableKBeanClass();
    }

    /**
     * Returns the Maven Publication associated with this KBean
     */
    public JkMavenPublication getPublication() {
        if (publication == null) {
            JkBuildable buildable = this.getRunbase().getBuildable();
            publication = JkMavenPublication.of(buildable);
            if (!JkUtilsString.isBlank(pub.moduleId)) {
                publication.setModuleId(pub.moduleId);
            }
            if (!JkUtilsString.isBlank(pub.version)) {
                publication.setVersion(pub.version);
            }
            if (pub.gitVersioning.enable) {
                publication.setVersionSupplier(
                        JkVersionFromGit.of(this.getBaseDir(), pub.gitVersioning.tagPrefix)::getVersionAsJkVersion);
            }

            this.pub.metadata.applyTo(publication);

            // Add Publish Repos from JKProperties
            publication.setRepos(getPublishReposFromProps());

            // Add artifacts declared in "publication.extraArtifacts"
            pub.extraArtifacts().forEach(publication::putArtifact);

            if (pub.parentBom) {
                configureForBom(publication);
            }
        }
        return publication;
    }

    public JkRepoSet createStandardOssrhRepos() {
        JkRepoProperties repoProperties = JkRepoProperties.of(this.getRunbase().getProperties());
        JkGpgSigner signer = JkGpgSigner.ofStandardProperties();
        return JkRepoSet.ofOssrhSnapshotAndRelease(repoProperties.getPublishUsername(),
                    repoProperties.getPublishPassword(), signer);
    }

    private void configureForBom(JkMavenPublication bomPublication) {
        bomPublication.customizeDependencies(deps -> JkDependencySet.of()); // No dependencies in BOM
        bomPublication.setPomPublicationOnly();  // No artifacts in BOM
        getRunbase().loadChildren(ProjectKBean.class).forEach(projectKBean -> {
            JkProject project = projectKBean.project;
            bomPublication.addManagedDependenciesInPom(project.getModuleId().toColonNotation(),
                    bomPublication.getVersion().toString());
        });
    }

    private JkRepoSet getPublishReposFromProps() {
        if (pub.predefinedRepo == PredefinedRepo.OSSRH) {
            return createStandardOssrhRepos();
        }
        JkRepoProperties repoProperties = JkRepoProperties.of(this.getRunbase().getProperties());
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

        @JkDoc("Comma separated list of license formated as <license name>=<license url>")
        @JkDepSuggest(versionOnly = true, hint =
                "Apache License V2.0=https://www.apache.org/licenses/LICENSE-2.0.html," +
                "MIT License=https://www.mit.edu/~amini/LICENSE.md")
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
                            String[] licenseItems = item.split("=");
                            String licenseName = licenseItems[0];
                            String licenseUrl = licenseItems.length > 1 ? licenseItems[1] : "";
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

        @JkDoc("Module id of the module to publish formatted as groupId:artifactId")
        public String moduleId;

        @JkDoc("The version of the module to publish")
        public String version;

        @JkDoc("If true, the publication will generate a BOM as its only artifact. \n" +
                "The BOM will point to the version of each child base that contains a project KBean.\n" +
                "Be caution to declare it with leading '_' as  '_@maven.pub.parentBom' in jeka.properties to not" +
                " propagate it to children.")
        public boolean parentBom;

        public final JkGitVersioning gitVersioning = JkGitVersioning.of();

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
