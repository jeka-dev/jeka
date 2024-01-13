package dev.jeka.core.tool.builtins.git;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.JkGit;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.self.SelfAppKBean;

@JkDoc({"Manages versioning of project (coming from ProjectJkBean) by extracting Git information.",
        "The version is inferred from git using following logic : ",
        "  - If git workspace is dirty (different than last commit), version values [branch]-SNAPSHOT",
        "  - If last commit contains a message containing [commentKeyword]xxxxx, version values xxxxx",
        "  - If last commit is tagged, version values [last tag on last commit]",
        "The inferred version can be  applied to project.publication.maven.version and project.publication.ivy.publication, " +
                "programmatically using 'handleVersioning' method."
})
public class GitKBean extends KBean {

    @JkDoc("Some likes to tag versions using a prefix (e.g. using 'v1.3.1' for tagging version '1.3.1'. In this case, " +
            "you can set this value to 'v' or whatever prefix.")
    public String versionTagPrefix = "";

    @JkDoc("If true, this Kbean will handle versioning og ProjectKBean or SelfAppKBean found in the JkRuntime.")
    public boolean handleVersioning;

    public final JkGit git = JkGit.of(getBaseDir());

    private JkVersionFromGit versionFromGit;

    @Override
    protected void init() {
        versionFromGit = JkVersionFromGit.of(getBaseDir(), versionTagPrefix);
        if (handleVersioning) {
            getRuntime().findInstanceOf(SelfAppKBean.class).ifPresent(selApp ->
                    versionFromGit.handleVersioning(selApp));
            getRuntime().find(ProjectKBean.class).ifPresent(projectKBean ->
                    versionFromGit.handleVersioning(projectKBean.project));
        }
    }

    @JkDoc("Performs a dirty check first, put a tag at the HEAD and push it to the remote repo." +
            " The user will be prompted to enter the tag name.")
    public void tagRemote() {
        git.tagRemote();
    }

    @JkDoc("Display version supplied to the project.")
    public void showVersion() {
        JkLog.info(gerVersionFromGit().getVersion());
    }

    @JkDoc("Handle versioning of the project managed in the projectKBean. " +
            "It is meant to be called from the property file cmd, prior other project#xxxxx commands. ")
    public void handleProjectVersioning() {
        getRuntime().find(ProjectKBean.class).ifPresent(projectKBean ->
                gerVersionFromGit().handleVersioning(projectKBean.project));
    }

    @JkDoc("Display last git tag in current branch")
    public void lastTag() {
        System.out.println(JkGit.of(getBaseDir()).setLogWithJekaDecorator(false).getLatestTag());
    }

    @JkDoc("Display all commit messages since last tag")
    public void lastCommitMessages() {
        JkGit.of(getBaseDir()).setLogWithJekaDecorator(false)
                .getCommitMessagesSinceLastTag().forEach(msg -> System.out.println("- " + msg));
    }

    /**
     * Gets the current version either from commit message if specified nor from git tag.
     */
    public String version() {
        return gerVersionFromGit().getVersion();
    }

    /**
     * Returns a configured {@link JkVersionFromGit}.
     */
    public JkVersionFromGit gerVersionFromGit() {
        return versionFromGit;
    }

}
