package dev.jeka.core.tool.builtins.scaffold;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;

import java.nio.file.Path;

/**
 * Provides method to generate a project skeleton (folder structure, configuration files, ....)
 */
@JkDoc("Generates project skeletons (folder structure and basic build files).")
public class ScaffoldKBean extends KBean {

    @JkDoc("If set then the wrapper shell script will delegate 'jekaw' call to jekaw script located in the specified folder")
    public String wrapDelegatePath;

    @JkDoc("Set the Jeka version to fetch for the wrapper. If null, it will use the same Jeka version than the running one.")
    public String wrapperJekaVersion;

    /**
     * In windows, we cannot pass arguments with breaking lines.
     * Uses
     */
    @JkDoc("Deprecated - Add extra content at the end of the template local.properties file.")
    public String localPropsExtraContent = "";

    @JkDoc("Add extra content at the end of the template local.properties file.")
    public Path localPropsExtraContentPath;

    public final JkScaffold scaffold = new JkScaffold(getBaseDir());

    @Override
    protected void init() {
        this.scaffold.setJekaClassCodeProvider(
                () -> JkUtilsIO.read(ScaffoldKBean.class.getResource("buildclass.snippet")));
        JkRepoSet repos = JkRepoProperties.of(getRuntime().getProperties()).getDownloadRepos();
        final JkDependencyResolver dependencyResolver = JkDependencyResolver.of(repos);
        this.scaffold.setDependencyResolver(dependencyResolver);
        this.scaffold.addLocalPropsFileContent(this.localPropsExtraContent);
        if (this.localPropsExtraContentPath != null) {
            String content = JkPathFile.of(localPropsExtraContentPath).readAsString();
            this.scaffold.addLocalPropsFileContent(content);
        }
    }

    @JkDoc("Generates project skeleton (folders and files necessary to work with the project).")
    public void run() {
        scaffold.run();
    }

    @JkDoc("Copies Jeka wrapper executable inside the project in order to be run in wrapper mode.")
    public void wrapper() {
        if (JkUtilsString.isBlank(wrapDelegatePath)) {
            scaffold.createStandardWrapperStructure();
        } else {
            scaffold.createWrapperStructureWithDelagation(wrapDelegatePath);
        }
    }

}
