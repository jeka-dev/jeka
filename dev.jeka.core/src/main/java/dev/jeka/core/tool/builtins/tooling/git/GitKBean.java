package dev.jeka.core.tool.builtins.tooling.git;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.api.tooling.git.JkVersionFromGit;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.base.BaseKBean;

@JkDoc("Provides project versioning by extracting Git information" + "\n" +
        "The version is inferred from git using following logic : "+ "\n" +
        "  - If git workspace is dirty (different than last commit), version values [branch]-SNAPSHOT"+ "\n" +
        "  - If last commit contains a message containing [commentKeyword]xxxxx, version values xxxxx"+ "\n" +
        "  - If last commit is tagged, version values [last tag on last commit]"+ "\n" +
        "The inferred version can be applied to project.publication.maven.version and project.publication.ivy.publication, " +
                "programmatically using 'handleVersioning' method."
)
public final class GitKBean extends KBean {

    @JkDoc("Some prefer to prefix version tags like 'v1.3.1' for version '1.3.1'. In such cases, this value can be set to 'v' or any chosen prefix")
    public String versionTagPrefix = "";

    @JkDoc("If true, this Kbean will handle versioning of ProjectKBean or BaseKBean found in the runbase.")
    public boolean handleVersioning;

    public final JkGit git = JkGit.of(getBaseDir());

    private JkVersionFromGit versionFromGit;

    @Override
    protected void init() {
        versionFromGit = JkVersionFromGit.of(getBaseDir(), versionTagPrefix);
        if (handleVersioning) {
            getRunbase().findInstanceOf(BaseKBean.class).ifPresent(selfKBean ->
                    versionFromGit.handleVersioning(selfKBean));
            getRunbase().find(ProjectKBean.class).ifPresent(projectKBean ->
                    versionFromGit.handleVersioning(projectKBean.project));
        }
    }

    @JkDoc("Performs a dirty check first, put a tag at the HEAD and push it to the remote repo." +
            " The user will be prompted to enter the tag name.")
    public void tagRemote() {
        git.tagRemote();
    }

    @JkDoc("Displays version supplied to the project.")
    public void showVersion() {
        JkLog.info(gerVersionFromGit().getVersion());
    }

    @JkDoc("Handles versioning of the project managed in the projectKBean. " +
            "It is meant to be called from the property file cmd, prior other project#xxxxx commands. ")
    public void handleProjectVersioning() {
        getRunbase().find(ProjectKBean.class).ifPresent(projectKBean ->
                gerVersionFromGit().handleVersioning(projectKBean.project));
    }

    @JkDoc("Displays last git tag in current branch")
    public void lastTag() {
        System.out.println(JkGit.of(getBaseDir()).setLogWithJekaDecorator(false).getLatestTag());
    }

    @JkDoc("Displays all commit messages since last tag")
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
