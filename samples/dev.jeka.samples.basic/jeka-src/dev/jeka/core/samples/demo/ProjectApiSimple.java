package dev.jeka.core.samples.demo;

import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectPackaging;
import dev.jeka.core.api.project.JkProjectPublications;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

/**
 * Simple build for a library project that is bundled as a FAT jar.<p/>
 * Execute <code>jeka #cleanPack</code> to buiild, test and create jars.
 */
class ProjectApiSimple extends KBean {

    final JkProject project = load(ProjectKBean.class).project;

    ProjectApiSimple() {
        project.flatFacade
                .setMainArtifactJarType(JkProjectPackaging.JarType.FAT); // Produce a fat Jar
        project.flatFacade.dependencies.compile
                .add("com.google.guava:guava:32.1.3-jre")
                .add("org.slf4j:slf4j-simple:2.0.9");
         project.flatFacade.addCompileOnlyDeps("org.projectlombok:lombok:1.18.30");
         project.flatFacade.dependencies.test
                 .add("org.junit.jupiter:junit-jupiter:5.10.1")
                 .add("org.mockito:mockito-core:5.7.0");
         project
                .setModuleId("org.examples:my-lib")
                .setVersion("0.1.0");
    }

    @JkDoc("Publish the built artifact (jar, sources and javadoc)")
    public void publish() {
        JkMavenPublication.of(project.asBuildable()).publish();
    }

}