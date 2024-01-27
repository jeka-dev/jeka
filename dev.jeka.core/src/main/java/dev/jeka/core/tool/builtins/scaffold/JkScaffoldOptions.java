package dev.jeka.core.tool.builtins.scaffold;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkRunbase;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Provides method to generate a project skeleton (folder structure, configuration files, ....)
 */
@JkDoc("Generates project skeletons (folder structure and basic build files).")
public class JkScaffoldOptions {

    @JkDoc("Set a specific jeka.version to include in jeka.properties.")
    private String jekaVersion;

    @JkDoc("Set a specific jeka.distrib.location to include in jeka.properties.")
    private String jekaLocation;

    @JkDoc("Set a specific jeka.distrib.repo to include in jeka.properties.")
    private String jekaDistribRepo;

    @JkDoc("Coma separated string representing properties to add to jeka.properties.")
    private String extraJekaProps = "";

    @JkDoc("Add extra content at the end of the template jeka.properties file.")
    private Path extraJekaPropsContentPath;

    public void configure(JkScaffold scaffold) {

        // Add sample code
        // todo : Sample code should be encapsulated
        scaffold.setJekaClassCodeProvider(
                () -> JkUtilsIO.read(JkScaffold.class.getResource("app.snippet"))
        );

        JkRepoSet repos = JkRepoProperties.of(JkRunbase.get(Paths.get("")).getProperties()).getDownloadRepos();
        final JkDependencyResolver dependencyResolver = JkDependencyResolver.of(repos);

        // add extra content to jeka.properties
        if (extraJekaProps != null) {
            Arrays.stream(extraJekaProps.split(",")).forEach(scaffold::addJekaPropValue);
        }
        if (extraJekaPropsContentPath != null) {
            String content = JkPathFile.of(extraJekaPropsContentPath).readAsString();
            scaffold.addJekaPropsFileContent(content);
        }

        scaffold
                .setDependencyResolver(dependencyResolver)
                .setJekaVersion(jekaVersion)
                .setJekaLocation(jekaLocation)
                .setJekaDistribRepo(jekaDistribRepo);
    }


}
