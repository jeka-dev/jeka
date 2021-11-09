package dev.jeka.core.tool.builtins.release;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDocPluginDeps;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.builtins.git.JkPluginGit;
import dev.jeka.core.tool.builtins.project.JkPluginProject;

import java.util.Optional;

@JkDoc({"Manage versioning of project from JkPluginProject by using Git.",
        "The version is inferred from git : ",
        "  - If git workspace is dirty (different than last commit), version values [branch]-SNAPSHOT",
        "  - If last commit contains a message containing [comment_version_prefix]xxxxx, version values xxxxx",
        "  - If last commit is tagged, version values [last tag on last commit]",
        "The inferred version is applied to project.publication.maven.version and project.publication.ivy.publication.",
        "After, If last commit message specifies a version and this version differs from tag, " +
                "last commit is tagged with specified version."})
@JkDocPluginDeps(JkPluginProject.class)
public class JkPluginVersionFromGit extends JkPlugin {

    public static final String TAG_TASK_NAME = "version-from-git-tag";

    @JkDoc("The prefix to use in commit message to specify a version.")
    public String commentVersionPrefix = "Release:";

    @JkDoc("Tags with following prefix. This may help to distinguish tags for versioning from others.")
    public String tagPrefixForVersion = "";

    @JkDoc("If true and a JkPluginProject project is bound to the build instance, the project will be configured for " +
            "publishing with the inferred version.")
    public boolean autoConfigureProject = true;

    @JkDoc("If true and autoConfigureProject, project will be configured to push tag after project.publication.publish() succeed.")
    public boolean tagAfterPublish = true;

    private JkGitProcess git;

    private transient JkVersion cachedVersion;

    protected JkPluginVersionFromGit(JkClass jkClass) {
        super(jkClass);
        JkPluginGit gitPlugin = jkClass.getPlugins().getOptional(JkPluginGit.class).orElse(null);
        git = gitPlugin != null ? gitPlugin.getGitProcess() : JkGitProcess.of(jkClass.getBaseDir());
    }

    @Override
    protected void afterSetup() {
        if (autoConfigureProject) {
            JkPluginProject projectPlugin = getJkClass().getPlugins().getOptional(JkPluginProject.class).orElse(null);
            if (projectPlugin == null) {
                return;
            }
            configure(projectPlugin.getProject(), tagAfterPublish);
        }
    }

    /**
     * Configure the specified project to use git version for publishing and tagging the repository.
     * @param tag If true, the repository will be tagged right after the project.pubmication.publish()
     */
    public void configure(JkProject project, boolean tag) {
        project.getPublication()
                .getMaven()
                    .setVersion(() -> version().toString())
                .__
                .getIvy()
                    .setVersion(() -> version().toString());
        if (tag) {
            project.getPublication().getPostActions().append(TAG_TASK_NAME, this::tagIfDiffers);
        }
    }

    public JkGitProcess git() {
        return git.clone();
    }

    /**
     * Gets the current version either from commit message if specified nor from git tag.
     */
    public JkVersion version() {
        if (cachedVersion != null) {
            return cachedVersion;
        }
        String currentTagVersion = git.getVersionFromTag(tagPrefixForVersion);
        String commitCommentVersion = git.extractSuffixFromLastCommitMessage(commentVersionPrefix);
        cachedVersion = JkVersion.of(Optional.ofNullable(commitCommentVersion).orElse(currentTagVersion));
        return cachedVersion;
    }

    /**
     * Tags git repository and push with the version specified in last git comment.
     * If no version is specified or the specified version is equals to the current tag, no tag will be set.
     */
    public boolean tagIfDiffers() {
        String commitCommentVersion = git.extractSuffixFromLastCommitMessage(commentVersionPrefix);
        if (commitCommentVersion == null) {
            return false;
        }
        String currentTagVersion = git.getVersionFromTag(tagPrefixForVersion);
        if (!commitCommentVersion.equals(currentTagVersion)) {
            JkLog.info("Tagging git with " + commitCommentVersion);
            git.tagAndPush(tagPrefixForVersion + commitCommentVersion);
            return true;
        }
        return false;
    }

    /**
     * {@link #version()} return is cached to avoid too many git call. Invoke this method to clear version cache.
     */
    public JkPluginVersionFromGit refresh() {
        cachedVersion = null;
        return this;
    }

    @JkDoc("Display inferred version on console.")
    public void showVersion() {
        JkLog.info(version().toString());
    }
}
