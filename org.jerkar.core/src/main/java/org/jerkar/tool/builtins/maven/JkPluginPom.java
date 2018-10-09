package org.jerkar.tool.builtins.maven;

import org.jerkar.api.system.JkException;
import org.jerkar.api.tooling.JkPom;
import org.jerkar.tool.JkRun;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkPlugin;

import java.nio.file.Files;
import java.nio.file.Path;

@JkDoc("Provides method to help migration from Maven.")
public class JkPluginPom extends JkPlugin {

    protected JkPluginPom(JkRun run) {
        super(run);
    }

    @JkDoc("Prints Java code for declaring dependencies on console based on pom.xml. The pom.xml file is supposed to be in root directory.")
    public void dependencyCode() {
        Path pomPath = owner.baseDir().resolve("pom.xml");
        if (!Files.exists(pomPath)) {
            throw new JkException("No pom file found at " + pomPath);
        }
        JkPom pom = JkPom.of(pomPath);
        System.out.println(pom.dependencies().withVersionProvider(pom.versionProvider()).toJavaCode(6));
    }
}
