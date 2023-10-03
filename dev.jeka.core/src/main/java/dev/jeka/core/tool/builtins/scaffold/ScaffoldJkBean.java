package dev.jeka.core.tool.builtins.scaffold;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;

import java.util.function.Consumer;

/**
 * Provides method to generate a project skeleton (folder structure, configuration files, ....)
 */
@JkDoc("Generates project skeletons (folder structure and basic build files).")
public class ScaffoldJkBean extends JkBean {

    private JkScaffolder scaffolder;

    @JkDoc("If set then the wrapper shell script will delegate 'jekaw' call to jekaw script located in the specified folder")
    public String wrapDelegatePath;

    @JkDoc("Set the Jeka version to fetch for the wrapper. If null, it will use the same Jeka version than the running one.")
    public String wrapperJekaVersion;

    @JkDoc("Add extra content at the end of the template local.properties file.")
    public String localPropsExtraContent = "";

    private JkConsumers<JkScaffolder> configurators = JkConsumers.of();

    private JkScaffolder scaffolder() {
        if (scaffolder != null) {
            return scaffolder;
        }
        this.scaffolder = new JkScaffolder(getBaseDir());
        this.scaffolder.setJekaClassCodeProvider(
                () -> JkUtilsIO.read(ScaffoldJkBean.class.getResource("buildclass.snippet")));
        JkRepoSet repos = JkRepoProperties.of(getRuntime().getProperties()).getDownloadRepos();
        final JkDependencyResolver dependencyResolver = JkDependencyResolver.of(repos);
        this.scaffolder.setDependencyResolver(dependencyResolver);
        this.scaffolder.addProjectPropsFileContent(this.localPropsExtraContent);
        configurators.accept(scaffolder);
        return scaffolder;
    }

    @JkDoc("Generates project skeleton (folders and files necessary to work with the project).")
    public void run() {
        scaffolder().run();
    }

    @JkDoc("Copies Jeka wrapper executable inside the project in order to be run in wrapper mode.")
    public void wrapper() {
        if (JkUtilsString.isBlank(wrapDelegatePath)) {
            scaffolder().createStandardWrapperStructure();
        } else {
            scaffolder().createWrapperStructureWithDelagation(wrapDelegatePath);
        }
    }

    /**
     * Adds a configurator that will be executed just before first use of underlying scaffolder.
     */
    public ScaffoldJkBean lately(Consumer<JkScaffolder> configurator) {
        this.configurators.append(configurator);
        return this;
    }

    /**
     * @deprecated Use {@link ScaffoldJkBean#lately(Consumer)} instead
     */
    @Deprecated
    public ScaffoldJkBean configure(Consumer<JkScaffolder> configurator) {
        return lately(configurator);
    }

}
