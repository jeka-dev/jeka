package dev.jeka.core.api.project.scaffold;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Provides features to scaffold projects.
 */
public class JkProjectScaffold extends JkScaffold {

    protected static final String BUILD_CLASS_PATH = JkConstants.JEKA_SRC_DIR + "/Build.java";

    protected static final String SIMPLE_STYLE_PROP = "project#layout.style=SIMPLE";

    public enum Template {
        BUILD_CLASS, PROPS, PLUGIN
    }

    protected final JkProject project;

    private boolean useSimpleStyle;

    public final List<String> compileDeps = new LinkedList<>();

    public final List<String> runtimeDeps = new LinkedList<>();

    public final List<String> testDeps = new LinkedList<>();

    private boolean generateLibsFolders;

    private Template template = Template.BUILD_CLASS;

    public JkProjectScaffold(JkProject project) {
        super(project.getBaseDir());
        this.project = project;
    }

    /**
     * Returns the template currently set for this scaffold
     */
    public Template getTemplate() {
        return template;
    }

    /**
     * Sets the template for this scaffold.
     */
    public JkProjectScaffold setTemplate(Template template) {
        this.template = template;
        return this;
    }

    public boolean isUseSimpleStyle() {
        return useSimpleStyle;
    }

    public JkProjectScaffold setUseSimpleStyle(boolean useSimpleStyle) {
        this.useSimpleStyle = useSimpleStyle;
        return this;
    }

    public JkProjectScaffold setGenerateLibsFolders(boolean generateLibsFolders) {
        this.generateLibsFolders = generateLibsFolders;
        return this;
    }

    @Override
    public void run() {
        configureScaffold();
        super.run();
        generateProjectStructure();
        generateDependencyTxt();
        if (generateLibsFolders) {
            generateLibsFolders();
        }
    }

    protected void removeFileEntry(String relativePath) {
        ListIterator<JkFileEntry> it = fileEntries.listIterator();
        while (it.hasNext()) {
            JkFileEntry fileEntry = it.next();
            if (relativePath.equals(fileEntry.relativePath)) {
                it.remove();
            }
        }
    }

    /**
     * Configures scaffold to creates project structure, including build class, according
     * the specified template.
     */
    protected void configureScaffold() {

        if (useSimpleStyle) {
            project.flatFacade().setLayoutStyle(JkCompileLayout.Style.SIMPLE);
            addJekaPropValue(SIMPLE_STYLE_PROP);
        }

        if (template == Template.BUILD_CLASS) {
            String code = readResource(JkProjectScaffold.class, "buildclass.snippet");
            addFileEntry(BUILD_CLASS_PATH, code);

        } else if (template == Template.PLUGIN) {
            String code = readResource(JkProjectScaffold.class, "buildclassplugin.snippet");
            code = code.replace("${jekaVersion}", JkInfo.getJekaVersion());
            addFileEntry(BUILD_CLASS_PATH, code);

        } else if (template == Template.PROPS) {
            addJekaPropValue(JkConstants.DEFAULT_KBEAN_PROP + "=project");
        }

    }

    /**
     * Generate the jeka-agnostic project skeleton (src dirs)
     */
    protected void generateProjectStructure() {
        JkLog.info("Create source directories.");
        JkCompileLayout prodLayout = project.compilation.layout;
        prodLayout.resolveSources().toList().forEach(JkPathTree::createIfNotExist);
        prodLayout.resolveResources().toList().forEach(JkPathTree::createIfNotExist);
        JkCompileLayout testLayout = project.testing.compilation.layout;
        testLayout.resolveSources().toList().forEach(JkPathTree::createIfNotExist);
        testLayout.resolveResources().toList().forEach(JkPathTree::createIfNotExist);

        // This is special scaffolding for plugin projects
        if (template == Template.PLUGIN) {
            Path breakingChangeFile = project.getBaseDir().resolve("breaking_versions.txt");
            String text = "## Next line means plugin 2.4.0.RC11 is not compatible with Jeka 0.9.0.RELEASE and above\n" +
                    "## 2.4.0.RC11 : 0.9.0.RELEASE   (remove this comment and leading '##' to be effective)";
            JkPathFile.of(breakingChangeFile).write(text);
            Path sourceDir =
                    project.compilation.layout.getSources().toList().get(0).getRoot();
            String pluginCode = JkUtilsIO.read(JkProjectScaffold.class.getResource("pluginclass.snippet"));
            JkPathFile.of(sourceDir.resolve("your/basepackage/XxxxxKBean.java"))
                    .createIfNotExist()
                    .write(pluginCode.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Creates a folder structure, local to the project, to store dependency library files.
     */
    private JkProjectScaffold generateLibsFolders() {
        Path libs = project.getBaseDir().resolve(JkProject.PROJECT_LIBS_DIR);
        JkPathFile.of(libs.resolve("readme.txt"))
                .fetchContentFrom(ProjectKBean.class.getResource("libs-readme.txt"));
        JkUtilsPath.createDirectories(libs.resolve("compile"));
        JkUtilsPath.createDirectories(libs.resolve("compile-only"));
        JkUtilsPath.createDirectories(libs.resolve("runtime-only"));
        JkUtilsPath.createDirectories(libs.resolve("test"));
        JkUtilsPath.createDirectories(libs.resolve("sources"));
        return this;
    }

    private void generateDependencyTxt() {
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
        JkPathFile.of(project.getBaseDir().resolve(JkProject.DEPENDENCIES_TXT_FILE))
                .createIfNotExist()
                .write(content);
    }



}
