package org.jerkar;

import org.jerkar.api.depmanagement.JkMavenPublication;
import org.jerkar.api.depmanagement.JkMavenPublicationInfo;
import org.jerkar.api.depmanagement.JkPublishRepos;
import org.jerkar.api.depmanagement.JkVersion;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.tool.JkOptions;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * Build settings shared across all Jerkar Java projects (core + plugins)
 */
public abstract class AbstractBuild extends JkJavaBuild {

    {
        this.pack.javadoc = true;
    }

    @Override
    public String javaSourceVersion() {
        return JkJavaCompiler.V6;
    }

    @Override
    public JkVersion version() {
        return JkVersion.name("0.5.0-SNAPSHOT");
    }

    @Override
    protected JkMavenPublication mavenPublication() {
        return super.mavenPublication().with(
                JkMavenPublicationInfo
                .of("Jerkar", "Build simpler, stronger, faster", "http://jerkar.github.io")
                .withScm("https://github.com/jerkar/jerkar.git").andApache2License()
                .andGitHubDeveloper("djeang", "djeangdev@yahoo.fr"));
    }

    @Override
    protected JkPublishRepos publishRepositories() {
        return JkPublishRepos.ossrh(JkOptions.get("repo.ossrh.username"),
                JkOptions.get("repo.ossrh.password"), pgp()).withUniqueSnapshot(true);
    }

}
