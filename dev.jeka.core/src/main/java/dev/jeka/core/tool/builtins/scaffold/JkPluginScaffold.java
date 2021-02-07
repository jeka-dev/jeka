package dev.jeka.core.tool.builtins.scaffold;

import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.builtins.repos.JkPluginRepo;

/**
 * Provides method to generate a project skeleton (folder structure, configuration files, ....)
 */
@JkDoc("Provides method to generate a project skeleton (folder structure and basic build files).")
public class JkPluginScaffold extends JkPlugin {

    private final JkScaffolder scaffolder;

    @JkDoc("If set then the wrapper shell script will delegate 'jekaw' call to jekaw script located in the specified folder")
    public String wrapDelegatePath;

    protected JkPluginScaffold(JkClass jkClass) {
        super(jkClass);
        this.scaffolder = new JkScaffolder(jkClass.getBaseDir());
        this.scaffolder.setJekaClassCode(JkUtilsIO.read(JkPluginScaffold.class.getResource("buildclass.snippet")));
    }

    public JkScaffolder getScaffolder() {
        return scaffolder;
    }

    @JkDoc("Generates project skeleton (folders and files necessary to work with the project).")
    public void run() {
        scaffolder.run();
    }

    @JkDoc("Copies Jeka executables inside the project in order to be run in embedded mode.")
    public void embed() {
        scaffolder.embed();
    }

    @JkDoc("Copies Jeka wrapper executable inside the project in order to be run in wrapper mode.")
    public void wrap() {
        if (JkUtilsString.isBlank(this.wrapDelegatePath)) {
            final JkPluginRepo repoPlugin = this.getJkClass().getPlugin(JkPluginRepo.class);
            final JkDependencyResolver dependencyResolver = JkDependencyResolver.ofParent(repoPlugin.downloadRepository().toSet());
            scaffolder.wrap(dependencyResolver);
        } else {
            scaffolder.wrapDelegate(this.wrapDelegatePath);
        }
    }

}
