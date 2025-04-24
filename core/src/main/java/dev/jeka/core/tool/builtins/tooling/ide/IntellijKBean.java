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

package dev.jeka.core.tool.builtins.tooling.ide;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.api.tooling.intellij.JkMiscXml;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.function.Consumer;

@JkDoc("Manages Intellij metadata files.")
@JkDocUrl("https://jeka-dev.github.io/jeka/reference/kbeans-intellij/")
public final class IntellijKBean extends KBean {

    // Flag for skipping modules.xml creation when testing.
    public static final String IML_SKIP_MODULE_XML_PROP = "jeka.intellij.iml.skip.moduleXml";

    @JkDoc("If true, dependency paths will be expressed relatively to $JEKA_REPO$ and $JEKA_HOME$ path variable instead of absolute paths.")
    private boolean useVarPath = true;

    @JkDoc("If true, the iml generation fails when a dependency can not be resolved. If false, it will be ignored " +
            "(only a warning will be notified).")
    private boolean failOnDepsResolutionError = !getRunbase().isForceMode();

    @JkDoc("The path where iml file must be generated. If null, Jeka will decide for a proper place. Mostly used by external tools.")
    public Path imlFile;

    @JkDoc("If mentioned, and jdkName is null, the generated iml will specify this jdkName")
    private String suggestedJdkName;

    @JkDoc("If mentioned, the generated iml will specify this jdkName")
    private String jdkName;

    @JkDoc("If true, sources will be downloaded will resolving dependencies")
    private boolean downloadSources = false;

    private JkImlGenerator imlGeneratorCache ;

    private final LinkedHashSet<String> projectLibraries = new LinkedHashSet<>();

    /**
     * @deprecated Use {@link #sync()} instead.
     */
    @JkDoc(value = "Deprecated: Use 'sync' instead.")
    @Deprecated
    public void iml() {
        sync();
    }

    @JkDoc("Generates IntelliJ [my-module].iml file.")
    public void sync() {
        JkLog.startTask("generate-iml");
        Path imlPath = getImlFile();
        adaptMiscXml();
        generateIml(imlPath);
        JkLog.endTask();
    }

    /**
     * Generate modules.xml files
     */
    @JkDoc("Generates ./idea/modules.xml file by grabbing all .iml files presents " +
            "in root or sub-directory of the project.")
    public void modulesXml() {
        IntelliJProject intelliJProject = IntelliJProject.of(getBaseDir());
        intelliJProject.regenerateModulesXml();
    }

    /**
     * @deprecated Use {@link #syncAll()} instead.
     */
    @Deprecated
    @JkDoc(value = "Deprecated: Use 'syncAll' instead.")
    public void allIml() {
        syncAll();
    }

    @JkDoc("Generates iml files on this folder and its descendant recursively.")
    public void syncAll() {
        JkPathTree.of(getBaseDir()).andMatching("**.iml").stream()
                .map(path -> {
                    if (path.getParent().getFileName().toString().equals(".idea")) {
                        return path.getParent().getParent();
                    } else {
                        return path.getParent();
                    }
                })
                .distinct()
                .forEach(this::generateImlExec);
    }

    @JkDoc("Re-init the project by deleting workspace.xml and regenerating .idea/modules.xml")
    public void initProject() {
        iml();
        IntelliJProject.of(getBaseDir()).deleteWorkspaceXml();
        modulesXml();
    }

    /**
     * Configures IML file that will be generated.
     */
    public IntellijKBean configureIml(Consumer<JkIml> imlConfigurator) {
        this.imlGenerator().configureIml(imlConfigurator);
        return this;
    }

    /**
     * In a multi-module project, a module that this one depends on might already include the Jeka dependency.
     * This method prevents adding the Jeka dependency directly to this module.
     */
    public IntellijKBean excludeJekaLib() {
        imlGenerator().setExcludeJekaLib(true);
        return this;
    }

    /**
     * Replaces the specified library with the specified module. The library is specified
     * by the end of its path. For example, '-foo.bar'  will replace 'mylibs/core-foo.jar'
     * by the specified module. Only the first matching lib is replaced.
     *
     * @see JkIml.Component#replaceLibByModule(String, String)
     */
    public IntellijKBean replaceLibByModule(String libName, String moduleName) {
        return configureIml(iml -> iml.component.replaceLibByModule(libName, moduleName));
    }

    /**
     * Adds a module dependency to the IntelliJ project configuration with the specified scope.
     *
     * @param moduleName The name of the module to be added as a dependency.
     * @param scope      The scope of the module dependency, defined by {@link JkIml.Scope}.
     * @return The current instance of {@link IntellijKBean} for method chaining.
     */
    public IntellijKBean addModule(String moduleName, JkIml.Scope scope) {
        return configureIml(iml -> iml.component.addModuleOrderEntry(moduleName, scope));
    }

    /**
     * Sets the <i>scope</i> and <i>exported</i> attribute to the specified module.
     *
     * @see JkIml.Component#setModuleAttributes(String, JkIml.Scope, Boolean)
     */
    public IntellijKBean setModuleAttributes(String moduleName, JkIml.Scope scope, Boolean exported) {
        return configureIml(iml -> iml.component.setModuleAttributes(moduleName, scope, exported));
    }

    /**
     * Sets the Jdk to be referenced in the generated <i>iml</i> file, if none is specified
     * by {@link IntellijKBean#jdkName}.
     * @param sdkName The JDK name as exists in Intellij SDKs.
     */
    public IntellijKBean setSuggestedJdk(String sdkName) {
        if (JkUtilsString.isBlank(jdkName)) {
            return configureIml(iml -> iml.component.setJdkName(sdkName));
        }
        return this;
    }

    public IntellijKBean addProjectLibrary(String xml) {
        this.projectLibraries.add(xml);
        return this;
    }

    private JkImlGenerator imlGenerator() {
        if (imlGeneratorCache != null) {
            return imlGeneratorCache;
        }
        JkImlGenerator imlGenerator = JkImlGenerator.of();
        imlGenerator
                .setBaseDir(this.getBaseDir())
                .setJekaSrcClasspath(this.getRunbase().getClasspath())
                .setIdeSupport(() -> IdeSupport.getProjectIde(getRunbase()))
                .setFailOnDepsResolutionError(this.failOnDepsResolutionError)
                .setDownloadSources(this.downloadSources)
                .setRunbaseDependencies(this.getRunbase().getFullDependencies())
                .setUseVarPath(useVarPath);
        if (!JkUtilsString.isBlank(jdkName) && !JkMiscXml.ofBaseDir(this.getBaseDir()).exists()) {
            imlGenerator.configureIml(iml -> iml.component.setJdkName(jdkName));
        } else if (!JkUtilsString.isBlank(suggestedJdkName) && !JkMiscXml.ofBaseDir(this.getBaseDir()).exists()) {
            imlGenerator.configureIml(iml -> iml.component.setJdkName(suggestedJdkName));
        }
        this.imlGeneratorCache = imlGenerator;
        return imlGenerator;
    }

    private void generateIml(Path imlPath) {
        // Determine if we are trying to generate an iml for the 'jeka-src' submodule
        String extensionLessFileName = JkUtilsString.substringBeforeLast(imlPath.getFileName().toString(), ".");
        boolean isForJekaSrcModule = JkConstants.JEKA_SRC_DIR.equals(extensionLessFileName) ||
                extensionLessFileName.endsWith("-" + JkConstants.JEKA_SRC_DIR);
        JkIml iml = imlGenerator().computeIml(isForJekaSrcModule);

        JkPathFile.of(imlPath)
                .deleteIfExist()
                .createIfNotExist()
                .write(iml.toDoc().toXml().getBytes(StandardCharsets.UTF_8));
        JkLog.info("Iml file generated at " + Paths.get("").toAbsolutePath().relativize(imlPath).normalize());
    }

    private void adaptMiscXml() {
        JkMiscXml miscXml = JkMiscXml.ofBaseDir(getBaseDir());
        if (!miscXml.exists()) {
            return;
        }
        if (!JkUtilsString.isBlank(jdkName)) {
            miscXml.setJdk(jdkName);
            JkLog.verbose("misc.xml modified for JDK project=" + jdkName);
        } else if (!JkUtilsString.isBlank(suggestedJdkName)) {
            miscXml.setJdk(suggestedJdkName);
            JkLog.verbose("misc.xml modified for JDK project=" + suggestedJdkName);
        }
    }

    private void generateImlExec(Path moduleDir) {
        JkLog.startTask("Generate iml file on '%s'", moduleDir);
        Main.exec(moduleDir, "intellij#iml", "-dci");
        JkLog.endTask();
    }

    private Path getImlFile() {
        return Optional.ofNullable(imlFile).orElse(JkImlGenerator.getImlFilePath(getBaseDir()));
    }

}