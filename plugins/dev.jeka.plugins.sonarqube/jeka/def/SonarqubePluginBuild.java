import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkEnv;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.core.tool.builtins.release.JkPluginVersionFromGit;
import dev.jeka.core.tool.builtins.repos.JkPluginGpg;

class SonarqubePluginBuild extends JkClass {

    private final JkPluginJava java = getPlugin(JkPluginJava.class);

    @Override
    protected void setup() {
        JkPlugin.setJekaPluginCompatibilityRange(java.getProject().getConstruction().getManifest(),
                "0.9.15.M2",
                "https://raw.githubusercontent.com/jerkar/sonarqube-plugin/master/breaking_versions.txt");
        java.getProject().simpleFacade()
                .setJvmTargetVersion(JkJavaVersion.V8)
                .mixResourcesAndSources()
                .setSimpleLayout()
                .setCompileDependencies(deps -> deps
                        .andFiles(JkLocator.getJekaJarPath())
                );

        java.getProject().getPublication().getMaven()
                .setModuleId("dev.jeka:sonarqube-plugin")
                .getPomMetadata()
                    .getProjectInfo()
                        .setName("Jeka plugin for Sonarqube")
                        .setDescription("A Jeka plugin for Jacoco coverage tool")
                        .setUrl("https://github.com/jerkar/sonarqube-plugin")
                    .__
                    .getScm()
                        .setUrl("https://github.com/jerkar/sonarqube-plugin").__
                        .addApache2License()
                        .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");

        // Make javadoc only for releases
        if (!JkVersion.of(java.getProject().getPublication().getVersion()).isSnapshot()) {
            java.pack.javadoc = true;
        }
    }

    public void cleanPack() {
        clean(); java.pack();
    }


}