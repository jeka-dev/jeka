package dev.jeka.core.tool.builtins.release;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDocPluginDeps;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.builtins.git.JkPluginGit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import java.util.Optional;

@JkDoc({"Manage versioning of project from JkPluginJava by using Git.",
        "The version is inferred from git : ",
        "  - If git workspace is dirty (different than last commit), version values [branch]-SNAPSHOT",
        "  - If last commit contains a message containing [comment_version_prefix]xxxxx, version values xxxxx",
        "  - If last commit is tagged, version values [last tag on last commit]",
        "The inferred version is applied to project.publication.maven.version and project.publication.ivy.publication.",
        "After, If last commit message specifies a version and this version differs from tag, " +
                "last commit is tagged with specified version."})
@JkDocPluginDeps(JkPluginJava.class)
public class JkPluginVersionFromGit extends JkPlugin {

    @JkDoc("The prefix to use in commit message to specify a version.")
    public String commentVersionPrefix = "Release:";

    @JkDoc("Tags with following prefix. This may help to distinguish tags for versioning from others.")
    public String tagPrefixForVersion = "";

    @JkDoc("If true, tag git with version specified in commit message. The tag is set and pushed after project.publication.publish() succeed.")
    public boolean tagAfterPublish = true;

    private JkGitProcess git;

    protected JkPluginVersionFromGit(JkClass jkClass) {
        super(jkClass);
        JkPluginGit gitPlugin = jkClass.getPlugins().getIfLoaded(JkPluginGit.class);
        git = gitPlugin != null ? gitPlugin.getGitProcess() : JkGitProcess.of(jkClass.getBaseDir());
    }

    @Override
    protected void afterSetup() {
        JkPluginJava java = getJkClass().getPlugins().getIfLoaded(JkPluginJava.class);
        if (java == null) {
            return;
        }
        JkVersion version = version();
        JkJavaProject project = java.getProject();
        project.getPublication()
                .getMaven()
                    .setVersion(version.toString())
                .__
                .getIvy()
                    .setVersion(version.toString());
        if (tagAfterPublish) {
            project.getPublication().getPostActions().append(this::tagIfDiffers);
        }
    }

    public JkGitProcess git() {
        return git.clone();
    }

    /**
     * Gets the current version either from commit message if specified nor from git tag.
     */
    public JkVersion version() {
        String currentTagVersion = git.getVersionFromTag(tagPrefixForVersion);
        String commitCommentVersion = git.extractSuffixFromLastCommitMessage(commentVersionPrefix);
        return JkVersion.of(Optional.ofNullable(commitCommentVersion).orElse(currentTagVersion));
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

    @JkDoc("Display inferred version on console.")
    public void showVersion() {
        JkLog.info(version().toString());
    }
}
