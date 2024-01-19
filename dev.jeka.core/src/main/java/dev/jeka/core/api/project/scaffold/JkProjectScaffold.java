package dev.jeka.core.api.project.scaffold;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * Provides features to scaffold projects.
 */
public class JkProjectScaffold {


    public enum BuildClassTemplate {
        NORMAL, SIMPLE_FACADE, PLUGIN, CODE_LESS, PURE_API
    }

    private final JkProject project;

    private final JkScaffold scaffold;

    private JkProjectScaffold(JkProject project, JkScaffold scaffold) {
        this.project = project;
        this.scaffold = scaffold;
    }

    public static JkProjectScaffold of(JkProject project, JkScaffold scaffold) {
        return new JkProjectScaffold(project, scaffold);
    }

    /**
     * Configures scaffold to creates project structure, including build class, according
     * the specified template.
     */
    public void configureScaffold(BuildClassTemplate template) {
        scaffold.setJekaClassCodeProvider( () -> {
            final String snippet;
            if (template == BuildClassTemplate.CODE_LESS) {
                return null;
            }
            if (template == BuildClassTemplate.NORMAL) {
                snippet = "buildclass.snippet";
            } else if (template == BuildClassTemplate.PLUGIN) {
                snippet = "buildclassplugin.snippet";
            } else if (template == BuildClassTemplate.PURE_API) {
                snippet = "buildclasspureapi.snippet";
            } else {
                snippet = "buildclassfacade.snippet";
            }
            String templateTxt = JkUtilsIO.read(JkProjectScaffold.class.getResource(snippet));
            String baseDirName = project.getBaseDir().getFileName().toString();
            return templateTxt.replace("${group}", baseDirName).replace("${name}", baseDirName);
        });
        scaffold.setClassFilename("Build.java");
        scaffold.extraActions.append( () -> this.scaffoldProjectStructure(template));
    }



    /**
     * Creates a folder structure, local to the project, to store dependency library files.
     */
    public JkProjectScaffold generateLocalLibsFolders() {
        scaffold.extraActions.append("Generate folders for local libs", () -> {
            Path libs = project.getBaseDir().resolve(JkConstants.JEKA_DIR)
                    .resolve(JkProject.PROJECT_LIBS_DIR);
            JkPathFile.of(libs.resolve("readme.txt"))
                    .fetchContentFrom(ProjectKBean.class.getResource("libs-readme.txt"));
            JkUtilsPath.createDirectories(libs.resolve("regular"));
            JkUtilsPath.createDirectories(libs.resolve("compile-only"));
            JkUtilsPath.createDirectories(libs.resolve("runtime-only"));
            JkUtilsPath.createDirectories(libs.resolve("test"));
            JkUtilsPath.createDirectories(libs.resolve("sources"));
        });
        return this;
    }

    /**
     * Creates the <li>project-dependencies.txt</li> containing the specified dependencies,
     */
    public JkProjectScaffold createProjectDependenciesTxt(List<String> compileDeps,
                                                          List<String> runtimeDeps,
                                                          List<String> testDeps) {
        if (compileDeps.isEmpty() && runtimeDeps.isEmpty() && testDeps.isEmpty()) {
            return this;
        }
        scaffold.extraActions.append("Generate project-dependencies.txt", () -> {
            List<String> lines = JkUtilsIO.readAsLines(JkProjectScaffold.class.getResourceAsStream("dependencies.txt"));
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                sb.append(line).append("\n");
                if (line.startsWith("== COMPILE") && !compileDeps.isEmpty()) {
                    compileDeps.forEach(extraDep -> sb.append(extraDep.trim()).append("\n"));
                }
                if (line.startsWith("== RUNTIME") && !runtimeDeps.isEmpty()) {
                    runtimeDeps.forEach(extraDep -> sb.append(extraDep.trim()).append("\n"));
                }
                if (line.startsWith("== TEST") && !testDeps.isEmpty()) {
                    testDeps.forEach(extraDep -> sb.append(extraDep.trim()).append("\n"));
                }
            }
            String content = sb.toString();
            JkPathFile.of(project.getBaseDir().resolve(JkConstants.JEKA_DIR)
                            .resolve(JkProject.PROJECT_DEPENDENCIES_TXT_FILE))
                    .createIfNotExist()
                    .write(content);
        });
        return this;
    }

    /*
     * Generates project structure, including
     * @param template
     */
    private void scaffoldProjectStructure(BuildClassTemplate template) {
        JkLog.info("Create source directories.");
        JkCompileLayout prodLayout = project.compilation.layout;
        prodLayout.resolveSources().toList().forEach(JkPathTree::createIfNotExist);
        prodLayout.resolveResources().toList().forEach(JkPathTree::createIfNotExist);
        JkCompileLayout testLayout = project.testing.compilation.layout;
        testLayout.resolveSources().toList().forEach(JkPathTree::createIfNotExist);
        testLayout.resolveResources().toList().forEach(JkPathTree::createIfNotExist);

        // This is special scaffolding for project pretending to be plugins for Jeka
        if (template == JkProjectScaffold.BuildClassTemplate.PLUGIN) {
            Path breakingChangeFile = project.getBaseDir().resolve("breaking_versions.txt");
            String text = "## Next line means plugin 2.4.0.RC11 is not compatible with Jeka 0.9.0.RELEASE and above\n" +
                    "## 2.4.0.RC11 : 0.9.0.RELEASE   (remove this comment and leading '##' to be effective)";
            JkPathFile.of(breakingChangeFile).write(text);
            Path sourceDir =
                    project.compilation.layout.getSources().toList().get(0).getRoot();
            String pluginCode = JkUtilsIO.read(ProjectKBean.class.getResource("pluginclass.snippet"));
            JkPathFile.of(sourceDir.resolve("your/basepackage/XxxxxJkBean.java"))
                    .createIfNotExist()
                    .write(pluginCode.getBytes(StandardCharsets.UTF_8));
        }
    }

}
