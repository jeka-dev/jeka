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

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.depmanagement.resolution.JkResolutionParameters;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

/**
 * This builds a Java library and publish it on a maven repo using Project plugin. A Java library means a jar that
 * is not meant to be consumed by end-user but as a dependency of other Java projects.<p>
 *
 * @author Jerome Angibaud
 */
public class SimpleProjectKBean extends KBean {

    @JkDoc("If true, skip execution of Integration tests.")
    public boolean skipIT;

    public String checkedValue;

    @JkInject
    private ProjectKBean projectKBean;

    @JkPostInit
    private void postInit(IntellijKBean intellijKBean) {
        intellijKBean.replaceLibByModule("dev.jeka.jeka-core.jar", "core")
                .setModuleAttributes("core", JkIml.Scope.COMPILE, false );
    }

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean)  {
        JkProject project = projectKBean.project;
        project.flatFacade.dependencies.compile
                .add("com.google.guava:guava:30.0-jre")
                .add( "com.sun.jersey:jersey-server:1.19.4");
        project.flatFacade.dependencies.runtime
                .add("com.github.djeang:vincer-dom:1.2.0");
        project.flatFacade.dependencies.test
                .add("org.junit.jupiter:junit-jupiter:5.10.1");
        project.flatFacade
                .addTestExcludeSuffixIf(skipIT, "IT");
        project
                .setJvmTargetVersion(JkJavaVersion.V8);
        project
                .dependencyResolver
                    .getDefaultParams()
                        .setConflictResolver(JkResolutionParameters.JkConflictResolver.STRICT);
        project
                .testing
                    .testProcessor
                        .setForkingProcess(false)
                    .engineBehavior
                        .setProgressDisplayer(JkTestProcessor.JkProgressStyle.FULL);
    }

    @JkPostInit(required = true)
    private void postInit(MavenKBean mavenKBean) {
        mavenKBean.customizePublication(mavenPublication ->
            mavenPublication
                .setModuleId("dev.jeka:sample-javaplugin")
                .setVersion("1.0-SNAPSHOT")
                .addRepos(JkRepo.of(getOutputDir().resolve("test-output/maven-repo")))  // Use a dummy repo for demo purpose

                // Published dependencies can be modified here from the ones declared in dependency management.
                // Here jersey-server is not supposed to be part of the API but only needed at runtime.
                .customizeDependencies(deps -> deps
                        .withTransitivity("com.sun.jersey:jersey-server", JkTransitivity.RUNTIME)));
    }



    public JkProject getProject() {
        return projectKBean.project;
    }

    public void cleanPackPublish() {
        projectKBean.clean();
        projectKBean.pack();
        load(MavenKBean.class).publishLocal();
    }

    public void checkValueIsA() {
        JkUtilsAssert.state("A".equals(checkedValue), "checkedValue field values %s and not 'A'.", checkedValue);
        JkUtilsAssert.state("foo".equals(getRunbase().getProperties().get("my.prop")),"Project property 'my.prop' not found.");
    }

    // For debugging purpose
    public void printIml() {
        ProjectKBean projectKBean = load(ProjectKBean.class);
        JkImlGenerator imlGenerator = JkImlGenerator.of().setIdeSupport(projectKBean::getJavaIdeSupport);
        String iml = imlGenerator.computeIml().toDoc().toXml();
        System.out.println(iml);
    }

    public void printMvn() {
        MavenKBean pluginPom = load(MavenKBean.class);
        pluginPom.migrateDeps();
    }

    public void showDependencies() {
        load(ProjectKBean.class).depTreeAsXml();
    }
    
    public static void main(String[] args) {
	    SimpleProjectKBean bean = JkInit.kbean(SimpleProjectKBean.class, args, "checkedValue=A");
        bean.cleanPackPublish();
        bean.checkValueIsA();
    }

}
