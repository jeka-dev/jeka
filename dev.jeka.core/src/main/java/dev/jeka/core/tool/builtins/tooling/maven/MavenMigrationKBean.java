package dev.jeka.core.tool.builtins.tooling.maven;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.maven.JkMavenProject;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;

@JkDoc("Provides method to help migration from Maven.")
public class MavenMigrationKBean extends KBean {

    @JkDoc("whitespace count to indentSpri dependency code.")
    public int codeIndent = 4;

    @JkDoc("Displays Java code for declaring dependencies based on pom.xml. The pom.xml file is supposed to be in root directory.")
    public void showDepsAsCode()  {
        JkLog.info(JkMavenProject.of(getBaseDir()).getDependencyAsJeKaCode(codeIndent));
    }

    @JkDoc("Displays project-dependencies content based on pom.xml. The pom.xml file is supposed to be in root directory.")
    public void showDepsAsTxt()  {
        JkLog.info(JkMavenProject.of(getBaseDir()).getDependenciesAsTxt());
    }

}
