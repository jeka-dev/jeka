import dev.jeka.core.CoreBuild;
import dev.jeka.core.api.depmanagement.publication.JkNexusRepos;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDefImport;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.core.tool.builtins.release.JkPluginVersionFromGit;

import java.util.Optional;

class MasterBuild extends JkClass {

    @JkDefImport("../dev.jeka.core")
    CoreBuild coreBuild;

    @JkDefImport("../plugins/dev.jeka.plugins.jacoco")
    JacocoPluginBuild jacocoBuild;

    @JkDefImport("../plugins/dev.jeka.plugins.sonarqube")
    JkClass sonarqubeBuild;

    @JkDefImport("../plugins/dev.jeka.plugins.springboot")
    JkClass springbootBuild;

    JkPluginVersionFromGit versionFromGit = getPlugin(JkPluginVersionFromGit.class);

    @Override
    protected void setup() throws Exception {
        versionFromGit.autoConfigureProject = false;
        coreBuild.runIT = true;
        getImportedJkClasses().getDirects().forEach(build -> {
            if (!versionFromGit.version().isSnapshot()) {     // Produce javadoc only for release
                build.getPlugins().getOptional(JkPluginJava.class).ifPresent(plugin -> plugin.pack.javadoc = true);
            }
        });
    }

    @JkDoc("Clean build of core and plugins + running all tests + publish if needed.")
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
        runSamples();
        runScaffoldsWithPlugins();
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

    @JkDoc("Convenient method to set Posix permission for all jekaw files on git.")
    public void setPosixPermissions() {
        JkPathTree.of("../samples").andMatching("*/jekaw", "**/jekaw").getFiles().forEach(path -> {
            JkLog.info("Setting exec permission on git for file " + path);
            JkProcess.ofCmdLine("git update-index --chmod=+x " + path).run();
        });
    }

    @JkDoc("Clean build of core + plugins bypassing tests.")
    public void buildFast() {
        getImportedJkClasses().getDirectPlugins(JkPluginJava.class).forEach(plugin -> {
            plugin.getProject().simpleFacade().setTestSkipped(true);
            plugin.getProject().getPublication().includeJavadocAndSources(false);
            plugin.getJkClass().clean();
            plugin.getProject().getPublication().pack();
        });
    }

    public void buildCore() {
        coreBuild.cleanPack();
    }

    public void runSamples()  {
        new SamplesTester().run();
    }

    public void runScaffoldsWithPlugins() {
        new PluginScaffoldTester().run();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(MasterBuild.class, args).make();
    }

    static class BuildFast {
        public static void main(String[] args) {
            JkInit.instanceOf(MasterBuild.class, args).buildFast();
        }
    }

}
