package dev.jeka.core.samples.demo;

import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.project.JkIdeSupportSupplier;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectPackaging;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;

/**
 * Simple build for a library project that is bundled as a FAT jar.<p/>
 * Execute <code>jeka #cleanPack</code> to buiild, test and create jars.
 */
class JkProjectApiSimple extends JkBean implements JkIdeSupportSupplier {

    private JkProject project() {
        JkProject project = JkProject.of();
        project.flatFacade()   /// Produce a fat Jar
                .setMainArtifactJarType(JkProjectPackaging.JarType.FAT)
                .addCompileDeps(
                        "com.google.guava:guava:32.1.3-jre",
                        "org.slf4j:slf4j-simple:2.0.9"
                )
                .addCompileOnlyDeps(
                        "org.projectlombok:lombok:1.18.30"
                )
                .addTestDeps(
                        "org.junit.jupiter:junit-jupiter:5.10.1",
                        "org.mockito:mockito-core:5.7.0"
                )
                .setPublishedModuleId("org.examples:my-lib")
                .setPublishedVersion("0.1.0");
        return project;
    }

    @JkDoc("Clean output directory then compile, test and create jar")
    public void cleanPack() {
        project().clean().pack();
    }

    @JkDoc("Display project info on console")
    public void info() {
        System.out.println(project().getInfo());
    }

    @JkDoc("Display dependency tree on console")
    public void depTree() {
        project().displayDependencyTree();
    }

    @JkDoc("Publish the built artifact (jar, sources and javadoc)")
    public void publish() {
        project().publication.publish();
    }

    @Override
    public JkIdeSupport getJavaIdeSupport() {
        return project().getJavaIdeSupport();
    }

}