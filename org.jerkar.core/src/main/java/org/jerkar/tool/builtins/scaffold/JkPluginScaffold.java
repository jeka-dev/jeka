package org.jerkar.tool.builtins.scaffold;

import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.tool.JkRun;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkPlugin;

/**
 * Provides method to generate a project skeleton (folder structure, configuration files, ....)
 */
@JkDoc("Provides method to generate a project skeleton (folder structure and basic build files).")
public class JkPluginScaffold extends JkPlugin {

    @JkDoc("If true, the Jerkar executables will be copied inside the project in order to be run in embedded mode.")
    public boolean embed;

    private final JkScaffolder scaffolder;

    protected JkPluginScaffold(JkRun run) {
        super(run);
        this.scaffolder = new JkScaffolder(run.baseDir(), false);
        this.scaffolder.setRunClassCode(JkUtilsIO.read(JkPluginScaffold.class.getResource("buildclass.snippet")));
    }

    public void addExtraAction(Runnable runnable) {
        this.scaffolder.extraActions.chain(runnable);
    }

    public void setRunClassClode(String code) {
        scaffolder.setRunClassCode(code);
    }

    @JkDoc("Generates project skeleton (folders and files necessary to work with the project).")
    public void run() {
       scaffolder.setEmbbed(embed);
       scaffolder.run();
    }
}
