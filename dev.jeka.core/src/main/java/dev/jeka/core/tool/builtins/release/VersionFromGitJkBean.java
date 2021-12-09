package dev.jeka.core.tool.builtins.release;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.builtins.git.GitJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

import java.util.Optional;

@JkDoc({"Manages versioning of projects by extracting Git information.",
        "The version is inferred from git : ",
        "  - If git workspace is dirty (different than last commit), version values [branch]-SNAPSHOT",
        "  - If last commit contains a message containing [comment_version_prefix]xxxxx, version values xxxxx",
        "  - If last commit is tagged, version values [last tag on last commit]",
        "The inferred version is applied to project.publication.maven.version and project.publication.ivy.publication.",
        "After, If last commit message specifies a version and this version differs from tag, " +
                "last commit is tagged with specified version."})
public class VersionFromGitJkBean extends JkBean {

    public static final String TAG_TASK_NAME = "version-from-git-tag";

    @JkDoc("The prefix to use in commit message to specify a version.")
    public String commentVersionPrefix = "Release:";

    @JkDoc("Tags with following prefix. This may help to distinguish tags for versioning from others.")
    public String tagPrefixForVersion = "";

    @JkDoc("If true and a ProjectJkBean project is bound to the build instance, the project will be configured for " +
            "publishing with the inferred version.")
    public boolean autoConfigureProject = true;

    @JkDoc("If true and autoConfigureProject, project will be configured to push tag after project.publication.publish() succeed.")
    public boolean tagAfterPublish = true;

    private JkGitProcess git;

    private transient JkVersion cachedVersion;

    protected VersionFromGitJkBean() {
        GitJkBean gitPlugin = getRuntime().getBeanOptional(GitJkBean.class).orElse(null);
        git = gitPlugin != null ? gitPlugin.getGitProcess() : JkGitProcess.of(getBaseDir());
    }

    @Override
    protected void postInit() {
        if (autoConfigureProject) {
            ProjectJkBean projectPlugin = getRuntime().getBeanOptional(ProjectJkBean.class).orElse(null);
            if (projectPlugin == null) {
                return;
            }
            configure(projectPlugin.getProject(), tagAfterPublish);
        }
    }

    /**
     * Configure the specified project to use git version for publishing and tagging the repository.
     * @param tag If true, the repository will be tagged right after the project.publication.publish()
     */
    public void configure(JkProject project, boolean tag) {
        project.getPublication().setVersion(() -> version().toString());
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
        cachedVersion = JkVersion.of(Optional.ofNullable(commitCommentVersion)
                .orElseGet(() -> git.getVersionFromTag(tagPrefixForVersion)));
        if (!cachedVersion.isSnapshot() && git.isWorkspaceDirty()) {
            cachedVersion = cachedVersion.toSnapshot();
        }
        return cachedVersion;
    }

    public String versionAsText() {
        return version().getValue();
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
    public VersionFromGitJkBean refresh() {
        cachedVersion = null;
        return this;
    }

    @JkDoc("Display inferred version on console.")
    public void showVersion() {
        JkLog.info(version().toString());
    }
}
