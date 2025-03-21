import dev.jeka.core.CoreCustom;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;
import dev.jeka.plugins.nexus.NexusKBean;

@JkDep("core/.idea/output/test")                         // To bootstrap jeka in IntelliJ
@JkDep("plugins/plugins.nexus/.idea/output/production")  // To bootstrap jeka in IntelliJ

@JkChildBase("core")        // Forces core to be initialized prior plugin runbases
@JkChildBase("plugins/*")
class Build extends KBean {

    @JkInject
    private CoreCustom coreCustom;

    @Override
    protected void init() {
        this.getRunbase().loadChildren(NexusKBean.class).forEach(this::postInit);
    }

    @JkDoc("Clean build of core and plugins + running all tests + publish if needed.")
    public void pack() {
        getRunbase().loadChildren(ProjectKBean.class).forEach(projectKBean -> {
            JkLog.startTask("pack-and-test %s", projectKBean.project.getBaseDir().getFileName());
            projectKBean.project.fullBuild();
            JkLog.endTask();
        });
        load(BaseKBean.class).test();
        MkDocsEnricher.run();
    }

    @JkDoc("Publishes all the artifacts in local repository.")
    public void publishLocal() {
        JkLog.startTask("publish-local");
        getRunbase().loadChildren(MavenKBean.class).forEach(MavenKBean::publishLocal);
        load(MavenKBean.class).publishLocal();
        JkLog.endTask();
    }

    @JkDoc("Publishes all artifact on OSSRH and publishes Jeka Docker image on DockerHub. " +
            "Should be executed from GitHub action only.")
    public void publish() {

        // Guard to publish only on GitHub Action, either on 'master' branch or tags.
        String branchOrTag = System.getenv("GITHUB_BRANCH");
        boolean doPublishOnOssrh = branchOrTag.startsWith("refs/tags/") || branchOrTag.equals("refs/heads/master");
        if (!doPublishOnOssrh) {
            JkLog.info("Current commit is not on master branch or tag: won't publish.");
            return;
        }

        JkLog.startTask("publish-ossrh");
        getRunbase().loadChildren(MavenKBean.class).forEach(MavenKBean::publish);
        load(MavenKBean.class).publish();  // publish BOM
        JkLog.endTask();

        /*
        JkLog.startTask("create-github-release");
        _dev.Github github = new _dev.Github();
        github.ghToken = githubToken;
        github.publishGhRelease();
        JkLog.endTask();
         */
        coreCustom.publishJekaDockerImage();
    }

    @JkDoc("Convenient method to set Posix permission for all jeka shell files on git.")
    public void setPosixPermissions() {
        JkPathTree.of("../samples").andMatching("*/jeka", "**/jeka").getFiles().forEach(path -> {
            JkLog.info("Setting exec permission on git for file " + path);
            JkGit.exec("update-index", "--chmod=+x", path);
        });
    }

    @JkPostInit(required = true)
    private void postInit(MavenKBean mavenKBean) {
        String version = coreCustom.load(ProjectKBean.class).project.getVersion().getValue();

        // Configure BOM publication
        JkMavenPublication bomPublication = mavenKBean.getPublication();
        bomPublication.customizeDependencies(deps -> JkDependencySet.of()); // No dependencies in BOM
        bomPublication.removeAllArtifacts();  // No artifacts in BOM
        getRunbase().loadChildren(ProjectKBean.class).forEach(projectKBean -> {
            JkProject project = projectKBean.project;
            bomPublication.addManagedDependenciesInPom(project.getModuleId().toColonNotation(), version);
        });

        bomPublication
                .setModuleId("dev.jeka:bom")
                .setVersion(version)
                .pomMetadata
                    .setProjectName("Jeka BOM")
                    .setProjectDescription("Provides versions for all artifacts in 'dev.jeka' artifact group");
    }

    @JkPostInit(required = true)
    private void postInit(NexusKBean nexusKBean) {
        nexusKBean.setRepoReadTimeout(60000);
    }

}
