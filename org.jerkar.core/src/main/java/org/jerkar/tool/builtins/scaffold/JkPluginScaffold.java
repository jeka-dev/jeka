package org.jerkar.tool.builtins.scaffold;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkDocPluginDeps;
import org.jerkar.tool.JkPlugin;

/**
 * proposes method to generate a project skeleton (folder structure, build artifacts, configuration files, ....)
 */
@JkDoc("Provides method to generate a project skeleton (folder structure and basic build files).")
public class JkPluginScaffold extends JkPlugin {

    @JkDoc("If true, the Jerkar executables will be copied inside the project in order to be run in embedded mode.")
    public boolean embed;

    private final JkScaffolder scaffolder;

    protected JkPluginScaffold(JkBuild build) {
        super(build);
        this.scaffolder = new JkScaffolder(build.baseDir(), false);
        this.scaffolder.setBuildClassCode(JkUtilsIO.read(JkPluginScaffold.class.getResource("buildclass.snippet")));
    }

    public void addExtraAction(Runnable runnable) {
        this.scaffolder.extraActions.chain(runnable);
    }

    public void setBuildClassClode(String code) {
        scaffolder.setBuildClassCode(code);
    }

    @JkDoc("Generates project skeleton (folders and files necessary to work with the project).")
    public void run() {
       scaffolder.setEmbbed(embed);
       scaffolder.run();
    }
}
