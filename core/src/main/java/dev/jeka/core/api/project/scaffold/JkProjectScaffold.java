/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.project.scaffold;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provides features to scaffold projects.
 */
public final class JkProjectScaffold extends JkScaffold {

    public static final String BUILD_CLASS_PATH = JkConstants.JEKA_SRC_DIR + "/Custom.java";

    public static final String SIMPLE_STYLE_PROP = "@project.layout.style=SIMPLE";

    public static final String MIX_SOURCES_RES = "@project.layout.mixSourcesAndResources=true";

    public enum Kind {
        REGULAR, PLUGIN, EMPTY
    }

    private final JkProject project;

    private boolean useSimpleStyle;

    private boolean mixSourcesAndResources;

    public final List<String> compileDeps = new LinkedList<>();

    public final List<String> compileOnlyDeps = new LinkedList<>();

    public final List<String> runtimeDeps = new LinkedList<>();

    public final List<String> testDeps = new LinkedList<>();

    public final List<String> versionDeps = new LinkedList<>();

    private boolean generateLibsFolders;

    private Kind kind = Kind.REGULAR;

    private final JkConsumers<JkProjectScaffold> customizers = JkConsumers.of();

    private JkProjectScaffold(JkProject project) {
        super(project.getBaseDir());
        this.project = project;
    }

    public static JkProjectScaffold of(JkProject project) {
        return new JkProjectScaffold(project);
    }

    /**
     * Registers for customizers that will be applied at #run execution.
     */
    public JkProjectScaffold addCustomizer(Consumer<JkProjectScaffold> customizer) {
        this.customizers.append(customizer);
        return this;
    }

    /**
     * Returns the template currently set for this scaffold
     */
    public Kind getTemplate() {
        return kind;
    }

    /**
     * Sets the template for this scaffold.
     */
    public JkProjectScaffold setKind(Kind kind) {
        this.kind = kind;
        return this;
    }


    public JkProjectScaffold setUseSimpleStyle(boolean useSimpleStyle) {
        this.useSimpleStyle = useSimpleStyle;
        return this;
    }

    public JkProjectScaffold setMixSourcesAndResources(boolean mixSourcesAndResources) {
        this.mixSourcesAndResources = mixSourcesAndResources;
        return this;
    }

    public JkProjectScaffold setGenerateLibsFolders(boolean generateLibsFolders) {
        this.generateLibsFolders = generateLibsFolders;
        return this;
    }

    public String getSrcRelPath() {
        return project.compilation.layout.getSources().getRootDirsOrZipFiles().get(0).toString();
    }

    public String getResRelPath() {
        return project.compilation.layout.getResources().getRootDirsOrZipFiles().get(0).toString();
    }

    public String getTestRelPath() {
        return project.testing.compilation.layout.getSources().getRootDirsOrZipFiles().get(0).toString();
    }

    public String getTestResPath() {
        return project.testing.compilation.layout.getResources().getRootDirsOrZipFiles().get(0).toString();
    }

    @Override
    public void run() {
        JkLog.startTask("scaffold-project");
        if (customizers.isEmpty()) {
            configureDefault();
        }
        customizers.accept(this);
        postConfigureScaffold();
        super.run();
        generateProjectStructure();
        generateProjectDeps();
        if (generateLibsFolders) {
            generateLibsFolders();
        }
        generateReadme();
        JkLog.endTask("Project generated.");
    }

    public void addCustomKbeanFileEntry() {
        String code = readResource(JkProjectScaffold.class, "custom-class.snippet");
        addFileEntry(BUILD_CLASS_PATH, code);
    }

    private void configureDefault() {
        addJekaPropValue(JkConstants.KBEAN_DEFAULT_PROP + "=project");
        addJekaPropsContent("@custom=on\n");
    }

    /*
     * Configures scaffold to create project structure, including build class, according
     * the specified template.
     */
    private void postConfigureScaffold() {
        if (useSimpleStyle) {
            project.flatFacade.setLayoutStyle(JkCompileLayout.Style.SIMPLE);
            addJekaPropValue(SIMPLE_STYLE_PROP);
        }
        if (mixSourcesAndResources) {
            project.flatFacade.setMixResourcesAndSources();
            addJekaPropValue(MIX_SOURCES_RES);
        }
        if (kind == Kind.REGULAR) {
            addJekaPropsContent("\n@project.pack.jarType=FAT\n" +
                    "@project.pack.detectMainClass=true\n");
            addCustomKbeanFileEntry();

        } else if (kind == Kind.PLUGIN) {
            String code = readResource(JkProjectScaffold.class, "custom-class-plugin.snippet");
            if (!UNSPECIFIED_JEKA_VERSION.equals(getJekaVersion())) {
                code = code.replace("${jekaVersion}", JkInfo.getJekaVersion());
            }
            addFileEntry(BUILD_CLASS_PATH, code);
        }
    }

    /**
     * Generate the jeka-agnostic project skeleton (src dirs)
     */
    private void generateProjectStructure() {
        JkCompileLayout prodLayout = project.compilation.layout;
        prodLayout.resolveSources().toList().forEach(JkPathTree::createIfNotExist);
        prodLayout.resolveResources().toList().forEach(JkPathTree::createIfNotExist);
        JkCompileLayout testLayout = project.test.compilation.layout;
        testLayout.resolveSources().toList().forEach(JkPathTree::createIfNotExist);
        testLayout.resolveResources().toList().forEach(JkPathTree::createIfNotExist);

        Path sourceDir =
                project.compilation.layout.resolveSources().toList().get(0).getRoot();
        Path testSourceDir =
                project.test.compilation.layout.resolveSources().toList().get(0).getRoot();

        // This is special scaffolding for plugin projects
        if (kind == Kind.PLUGIN) {
            Path breakingChangeFile = project.getBaseDir().resolve("breaking_versions.txt");
            String text = "## Next line means plugin 2.4.0.RC11 is not compatible with Jeka 0.9.0.RELEASE and above\n" +
                    "## 2.4.0.RC11 : 0.9.0.RELEASE   (remove this comment and leading '##' to be effective)";
            JkPathFile.of(breakingChangeFile).write(text);

            String pluginCode = JkUtilsIO.read(JkProjectScaffold.class.getResource("plugin-class.snippet"));
            JkPathFile.of(sourceDir.resolve("your/basepackage/XxxxxKBean.java"))
                    .createIfNotExist()
                    .write(pluginCode.getBytes(StandardCharsets.UTF_8));

        } else if (kind == Kind.REGULAR) {
            String mainClass = JkUtilsIO.read(JkProjectScaffold.class.getResource("main-class.snippet"));
            JkPathFile.of(sourceDir.resolve("app/Main.java"))
                    .createIfNotExist()
                    .write(mainClass.getBytes(StandardCharsets.UTF_8));

            String testClass = JkUtilsIO.read(JkProjectScaffold.class.getResource("test-class.snippet"));
            JkPathFile.of(testSourceDir.resolve("app/MainTest.java"))
                    .createIfNotExist()
                    .write(testClass.getBytes(StandardCharsets.UTF_8));
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

    private void generateProjectDeps() {
        InputStream inputStream = JkProjectScaffold.class.getResourceAsStream(JkProject.PROJECT_DEPENDENCIES_FILE);
        List<String> lines = inputStream == null ? Collections.emptyList() : JkUtilsIO.readAsLines(inputStream);
        StringBuilder sb = new StringBuilder();
        if (testDeps.isEmpty()) {
            testDeps.addAll(getJUnitDeps());
            versionDeps.add("org.junit:junit-bom:" + JUNIT_VERSION + "@pom");
        }
        for (String line : lines) {
            sb.append(line).append("\n");
            if (line.startsWith("[compile]") && !compileDeps.isEmpty()) {
                compileDeps.forEach(extraDep -> sb.append(extraDep.trim()).append("\n"));
            }
            if (line.startsWith("[compile-only]") && !compileOnlyDeps.isEmpty()) {
                compileOnlyDeps.forEach(extraDep -> sb.append(extraDep.trim()).append("\n"));
            }
            if (line.startsWith("[runtime]") && !runtimeDeps.isEmpty()) {
                runtimeDeps.forEach(extraDep -> sb.append(extraDep.trim()).append("\n"));
            }
            if (line.startsWith("[test]")) {
                testDeps.forEach(extraDep -> sb.append(extraDep.trim()).append("\n"));
            }
            if (line.startsWith("[version]") && !versionDeps.isEmpty()) {
                versionDeps.forEach(extraDep -> sb.append(extraDep.trim()).append("\n"));
            }
        }
        String content = sb.toString();
        JkPathFile.of(project.getBaseDir().resolve(JkProject.PROJECT_DEPENDENCIES_FILE))
                .createIfNotExist()
                .write(content);
    }

    private void generateReadme() {
        String content = JkUtilsIO.read(JkProjectScaffold.class.getResource("README.md"));
        JkPathFile.of(project.getBaseDir().resolve("README.md")).createIfNotExist().write(content);
    }

}
