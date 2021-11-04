import dev.jeka.core.CoreBuild;
import dev.jeka.core.api.depmanagement.publication.JkNexusRepos;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDefImport;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.core.tool.builtins.release.JkPluginVersionFromGit;

class MasterBuild extends JkClass {

    @JkDefImport("../dev.jeka.core")
    CoreBuild coreBuild;

    @JkDefImport("../plugins/dev.jeka.plugins.jacoco")
    JacocoPluginBuild jacocoBuild;

    @JkDefImport("../plugins/dev.jeka.plugins.sonarqube")
    JkClass sonarqubeBuild;

    JkPluginVersionFromGit versionFromGit = getPlugin(JkPluginVersionFromGit.class);

    @Override
    protected void setup() throws Exception {
        versionFromGit.autoConfigureProject = false;
        coreBuild.runIT = true;
        getImportedJkClasses().getDirects().forEach(build -> {
            if (!versionFromGit.version().isSnapshot()) {     // Produce javadoc only for release
                JkPluginJava pluginJava = build.getPlugins().getIfLoaded(JkPluginJava.class);
                if (pluginJava != null) {
                    pluginJava.pack.javadoc = true;
                }
            }
        });
    }

    public void make() {
        JkLog.startTask("Building core and plugins");
        getImportedJkClasses().getDirects().forEach(build -> {
            JkLog.startTask("Building " + build);
            JkJavaProject project = build.getPlugin(JkPluginJava.class).getProject();
            versionFromGit.configure(project, false);
            build.clean();
            project.getPublication().pack();
            JkLog.endTask();
        });
        JkLog.endTask();
        JkLog.startTask("Running samples");
        setPosixPermissions();
        runSamples();
        JkLog.endTask();
        JkLog.startTask("Publishing");
        JkGitProcess git = JkGitProcess.of(this.getBaseDir());
        String branch = git.getCurrentBranch();
        if (branch.equals("master") && !versionFromGit.version().isSnapshot()) {
            getImportedJkClasses().getDirects().forEach(build -> build.getPlugin(JkPluginJava.class).publish());
            JkNexusRepos.ofUrlAndCredentials(coreBuild.getPlugin(JkPluginJava.class).getProject()
                    .getPublication().findFirstRepo());
        }
        JkLog.endTask();
    }

    public void setPosixPermissions() {
        JkPathTree.of("../samples").andMatching("*/jekaw", "**/jekaw").getFiles().forEach(path -> {
            JkLog.info("Setting exec permission on git for file " + path);
            JkProcess.ofCmdLine("git update-index --chmod=+x " + path).run();
        });
    }

    public void buildCore() {
        coreBuild.cleanPack();
    }

    public void buildPlugins() {
        jacocoBuild.cleanPack();
    }

    public void runSamples()  {
        new SamplesRunner().run();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(MasterBuild.class, args).make();
    }

}
