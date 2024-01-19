package dev.jeka.core.tool.builtins.scaffold;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;

import java.nio.file.Path;

/**
 * Provides method to generate a project skeleton (folder structure, configuration files, ....)
 */
@JkDoc("Generates project skeletons (folder structure and basic build files).")
public class ScaffoldKBean extends KBean {

    @JkDoc("Set a specific jeka.version to include in jeka.properties.")
    private String jekaVersion;

    @JkDoc("Set a specific jeka.distrib.location to include in jeka.properties.")
    private String jekaLocation;

    @JkDoc("Set a specific jeka.distrib.repo to include in jeka.properties.")
    private String jekaDistribRepo;

    /**
     * In windows, we cannot pass arguments with breaking lines.
     * Use {@link #jekaPropsExtraContentPath} instead.
     */
    @JkDoc("Deprecated - Add extra content at the end of the template local.properties file.")
    private String jekaPropsExtraContent = "";

    @JkDoc("Add extra content at the end of the template local.properties file.")
    private Path jekaPropsExtraContentPath;

    @JkDoc(hide = true)
    public final JkScaffold scaffold = JkScaffold.of(getBaseDir());

    @Override
    protected void init() {
        this.scaffold.setJekaClassCodeProvider(

                // todo : Sample code should be encapsulated
                () -> JkUtilsIO.read(JkScaffold.class.getResource("buildclass.snippet")));
        JkRepoSet repos = JkRepoProperties.of(getRunbase().getProperties()).getDownloadRepos();
        final JkDependencyResolver dependencyResolver = JkDependencyResolver.of(repos);
        this.scaffold
                .setDependencyResolver(dependencyResolver)
                .addLocalPropsFileContent(this.jekaPropsExtraContent)
                .setJekaVersion(jekaVersion)
                .setJekaLocation(jekaLocation)
                .setJekaDistribRepo(jekaDistribRepo);
        if (this.jekaPropsExtraContentPath != null) {
            String content = JkPathFile.of(jekaPropsExtraContentPath).readAsString();
            this.scaffold.addLocalPropsFileContent(content);
        }
    }

    @JkDoc("Generates project skeleton (folders and files necessary to work with the project).")
    public void run() {
        scaffold.run();
    }

}
