package dev.jeka.core.tool.builtins.git;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkPrompt;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

import java.util.Optional;

@JkDoc("Provides common Git scripts/commands out of the box.")
public class GitJkBean extends JkBean {

    @JkDoc({"Manages versioning of projects by extracting Git information.",
            "The version is inferred from git : ",
            "  - If git workspace is dirty (different than last commit), version values [branch]-SNAPSHOT",
            "  - If last commit contains a message containing [commentKeyword]xxxxx, version values xxxxx",
            "  - If last commit is tagged, version values [last tag on last commit]",
            "The inferred version is applied to project.publication.maven.version and project.publication.ivy.publication.",
            "After, if last commit message specifies a version and this version differs from tag, " +
                    "last commit is tagged with specified version."})
    public final ProjectVersionProvider projectVersionSupplier = new ProjectVersionProvider();

    private final JkGitProcess git;

    protected GitJkBean() {
        git = JkGitProcess.of(getBaseDir());
        if (projectVersionSupplier.on) {
            ProjectJkBean projectPlugin = getRuntime().getBeanOptional(ProjectJkBean.class).orElse(null);
            if (projectPlugin == null) {
                return;
            }
            projectPlugin.configure(project -> projectVersionSupplier.configure(project,
                    projectVersionSupplier.tagAfterPublish));
        }
    }

    @JkDoc("Perform a dirty check first then put a tag at the HEAD and push it to remote.")
    public void tagRemote() {
        JkGitProcess aGit = git.copy().setLogCommand(false).setLogOutput(false);
        System.out.println("Existing tags on origin :");
        aGit.copy().setLogOutput(true).exec("ls-remote", "--tag", "--sort=creatordate", "origin");
        if (aGit.isWorkspaceDirty()) {
            System.out.println("Git workspace is dirty. Please clean your Git workspace and retry");
            return;
        }
        if (!aGit.isRemoteEqual()) {
            System.out.println("The current tracking branch is not aligned with the remote. Please update/push and retry.");
            return;
        }
        System.out.println("You are about to tag commit : " + git.getCurrentCommit());
        final String newTag = JkPrompt.ask("Enter new tag : ");
        aGit.setLogCommand(true).tagAndPush(newTag);
    }

    @JkDoc("Display version supplied to the project.")
    public void showVersion() {
        if (!projectVersionSupplier.on) {
            JkLog.warn("The project is not configured to get its version from Git. Use git#projectVersionSupplier.on=true");
        } else {
            JkLog.info(projectVersionSupplier.version().toString());
        }
    }

    public JkGitProcess getGitProcess() {
        return git;
    }

    public class ProjectVersionProvider {

        @JkDoc("If true and a ProjectJkBean project is bound to the build instance, the project will be configured for " +
                "publishing with version tagged in GIT")
        public boolean on = false;

        public static final String TAG_TASK_NAME = "version-from-git-tag";

        @JkDoc("The keyword to use in commit message to order a tag (e.g. 'Release:1.0.2' will put a tag '1.0.2')")
        public String commentKeyword = "Release:";

        @JkDoc("If true and autoConfigureProject, project will be configured to push tag after project.publication.publish() succeed.")
        public boolean tagAfterPublish = true;

        private transient JkVersion cachedVersion;

        /**
         * Tags git repository and push with the version specified in last git comment.
         * If no version is specified or the specified version is equals to the current tag, no tag will be set.
         */
        public boolean tagIfDiffers() {
            String commitCommentVersion = git.extractSuffixFromLastCommitMessage(commentKeyword);
            if (commitCommentVersion == null) {
                return false;
            }
            String currentTagVersion = git.getVersionFromTag("");
            if (!commitCommentVersion.equals(currentTagVersion)) {
                JkLog.info("Tagging git with " + commitCommentVersion);
                git.tagAndPush(commitCommentVersion);
                return true;
            }
            return false;
        }

        /**
         * Gets the current version either from commit message if specified nor from git tag.
         */
        public JkVersion version() {
            if (cachedVersion != null) {
                return cachedVersion;
            }
            boolean dirty = GitJkBean.this.git.isWorkspaceDirty();
            if (dirty) {
                cachedVersion = JkVersion.of(git.getCurrentBranch()).toSnapshot();
            } else {
                String commitCommentVersion = git.extractSuffixFromLastCommitMessage(commentKeyword);
                cachedVersion = JkVersion.of(Optional.ofNullable(commitCommentVersion)
                        .orElseGet(() -> git.getVersionFromTag("")));
            }
            JkLog.info("Version inferred from Git:" + cachedVersion);
            return cachedVersion;
        }

        /**
         * Configure the specified project to use git version for publishing and tagging the repository.
         * @param tag If true, the repository will be tagged right after the project.publication.publish()
         */
        public void configure(JkProject project, boolean tag) {
            project.publication.setVersion(() -> version().toString());
            if (tag) {
                project.publication.postActions.append(TAG_TASK_NAME, this::tagIfDiffers);
            }
        }

        public String versionAsText() {
            return version().getValue();
        }

        public void refresh() {
            cachedVersion = null;
        }

    }

}
