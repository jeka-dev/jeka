package dev.jeka.core.tool.builtins.maven;

import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.tooling.JkPom;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkPlugin;

import java.nio.file.Files;
import java.nio.file.Path;

@JkDoc("Provides method to help migration from Maven.")
public class JkPluginPom extends JkPlugin {

    protected JkPluginPom(JkCommandSet run) {
        super(run);
    }

    @JkDoc("Prints Java code for declaring dependencies on console based on pom.xml. The pom.xml file is supposed to be in root directory.")
    public void dependencyCode() {
        Path pomPath = getCommandSet().getBaseDir().resolve("pom.xml");
        if (!Files.exists(pomPath)) {
            throw new JkException("No pom file found at " + pomPath);
        }
        JkPom pom = JkPom.of(pomPath);
        System.out.println(pom.getDependencies().withVersionProvider(pom.getVersionProvider()).toJavaCode(6));
    }
}
