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
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.intellij.*;
import dev.jeka.core.api.utils.JkUtilsJdk;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@JkDoc("Manages Intellij metadata files.")
@JkDocUrl("https://jeka-dev.github.io/jeka/reference/kbeans-intellij/")
public final class IntellijKBean extends KBean {

    private static final String PROJECT_JAVA_VERSION_PROP = "@project.javaVersion";

    /**
     * Determines which IntelliJ `.iml` file should be synced when there are
     * two `.iml` files present:
     *
     * <ul>
     *   <li>One for <code>jeka-src</code></li>
     *   <li>One for the project</li>
     * </ul>
     *
     * The <i>SyncFocus</i> specifies which of these `.iml` files will be prioritized
     * for synchronization.
     */
    public enum SyncFocus {
        JEKA_SRC, PROJECT
    }

    // Flag for skipping modules.xml creation when testing.
    public static final String IML_SKIP_MODULE_XML_PROP = "jeka.intellij.iml.skip.moduleXml";

    @JkDoc("If true, dependency paths will be expressed relatively to $JEKA_REPO$ and $JEKA_HOME$ path variable instead of absolute paths.")
    private boolean useVarPath = true;

    @JkDoc("If true, the iml generation fails when a dependency can not be resolved. If false, it will be ignored " +
            "(only a warning will be notified).")
    private boolean failOnDepsResolutionError = !getRunbase().isForceMode();

    @JkDoc(hide = true, value = "The path where iml file must be generated. If null, Jeka will decide for a proper place. Mostly used by external tools.")
    public Path imlFile;

    @JkDoc(hide = true, value = "If mentioned, and jdkName is null, the generated iml will specify this jdkName")
    @Deprecated
    public String suggestedJdkName;

    @JkDoc("Experimental: If true, 'jeka-src' will have its own IntelliJ module.")
    private boolean splitModule;

    @JkDoc("Experimental: If 'jeka-src' is in its own module, the 'sync' action can target it specifically.")
    private SyncFocus focus = null;

    @JkDoc(hide = true, value = "If mentioned, the generated for jekaSrc iml will specify this jdkName.")
    @Deprecated
    private String jdkName;

    @JkDoc("If true, sources will be downloaded will resolving dependencies.")
    private boolean downloadSources = false;

    @JkDoc(hide = true, value="Specifiy the intellij project dir (may be different than module root dir")
    private Path projectRootDir;

    private final JkConsumers<JkIml> imlCustomizers = JkConsumers.of();

    /**
     * @deprecated Use {@link #sync()} instead.
     */
    @JkDoc(hide = true, value = "Deprecated: Use 'sync' instead.")
    @Deprecated
    public void iml() {
        sync();
    }

    @JkDoc("Generates IntelliJ [my-module].iml file.")
    public void sync() {
        JkLog.startTask("generate-iml");
        FocusResult result = getFocusResult();
        Path regularImlPath = getImlFile();
        Path separatedJekasrcImlPath = getJekaSrcImlFile();
        JkModulesXml modulesXml = getModulesXml();
        if (!shouldSkipModulesXml()) {   // only for nor creating modules.xml during tests
            modulesXml.createIfAbsentOrInvalid();
        }
        SdkResolver sdkResolver = sdkResolver();
        if (result.shouldGenerateMergedIml()) {
            JkLog.debug("Intellij sync mode 1");
            generateSingleIml(regularImlPath, sdkResolver);
            if (!shouldSkipModulesXml()) {
                modulesXml.addImlIfNeeded(regularImlPath);
            }
            modulesXml.removeIfNeeded(separatedJekasrcImlPath);
            JkUtilsPath.deleteQuietly(separatedJekasrcImlPath, true);
        } else {
            if (result.specificJekaSrcIml) {
                if (result.specificProjectIml) {
                    JkLog.debug("Intellij sync mode é");
                    generateJekaSrcOnlyIml(separatedJekasrcImlPath, sdkResolver, false);
                    generateProjectOnlyIml(regularImlPath, sdkResolver);
                    modulesXml.addImlIfNeeded(regularImlPath);
                    modulesXml.addImlIfNeeded(separatedJekasrcImlPath);
                } else  {
                    JkLog.debug("Intellij sync mode 3");
                    generateJekaSrcOnlyIml(separatedJekasrcImlPath, sdkResolver, true);
                    modulesXml.addImlIfNeeded(separatedJekasrcImlPath);

                    // Don't remove the regular .iml because it will turn in a single xxx-jeka project in intellij.
                    // For using with Maven, the user may remove it manually, from the 'project settings' menu
                    modulesXml.removeIfNeeded(regularImlPath);
                    JkUtilsPath.deleteQuietly(regularImlPath, true);
                }
            } else  {
                JkLog.debug("Intellij sync mode 4");
                generateProjectOnlyIml(regularImlPath, sdkResolver);
                modulesXml.addImlIfNeeded(regularImlPath);
                modulesXml.removeIfNeeded(separatedJekasrcImlPath);
                JkUtilsPath.deleteQuietly(separatedJekasrcImlPath, true);
            }
        }
        JkLog.endTask();
    }

    /**
     * Generate modules.xml files
     */
    @JkDoc("Generates ./idea/modules.xml file by grabbing all .iml files presents " +
            "in root or sub-directory of the project.")
    public void modulesXml() {
        IntelliJProject intelliJProject = IntelliJProject.of(projectRootDir(getBaseDir()));
        intelliJProject.regenerateModulesXml();
    }

    /**
     * @deprecated Use {@link #syncAll()} instead.
     */
    @Deprecated
    @JkDoc(hide = true, value = "Deprecated: Use 'syncAll' instead.")
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
        IntelliJProject.of(projectRootDir(getBaseDir())).deleteWorkspaceXml();
        modulesXml();
    }

    /**
     * Configures IML file that will be generated.
     * @deprecated use {@link #customizeIml(Consumer)} instead
     */
    @Deprecated
    public IntellijKBean configureIml(Consumer<JkIml> imlCustomizer) {
        return customizeIml(imlCustomizer);
    }

    /**
     * Configures IML file that will be generated.
     */
    public IntellijKBean customizeIml(Consumer<JkIml> imlCustomizer) {
        imlCustomizers.append(imlCustomizer);
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

    private JkImlGenerator imlGenerator() {
        JkImlGenerator imlGenerator = JkImlGenerator.of();
        imlGenerator
                .setBaseDir(this.getBaseDir())
                .setJekaSrcClasspath(this.getRunbase().getClasspath())
                .setIdeSupport(() -> IdeSupport.getProjectIde(getRunbase()))
                .setFailOnDepsResolutionError(this.failOnDepsResolutionError)
                .setDownloadSources(this.downloadSources)
                .setRunbaseDependencies(this.getRunbase().getFullDependencies())
                .setUseVarPath(useVarPath);
        return imlGenerator;
    }

    private void generateImlExec(Path moduleDir) {
        JkLog.startTask("Generate iml file on '%s'", moduleDir);
        Main.exec(moduleDir, "intellij#iml", "-dci");
        JkLog.endTask();
    }

    private Path getImlFile() {
        return Optional.ofNullable(imlFile).orElse(findRegularImlFilePath());
    }

    private boolean hasProjectWithDistinctJavaVersion() {
        Optional<ProjectKBean> optionalProjectKBean = find(ProjectKBean.class);
        if (!optionalProjectKBean.isPresent()) {
            return false;
        }
        ProjectKBean projectKBean = optionalProjectKBean.get();
        JkJavaVersion projectJavaVersion = projectKBean.project.getJvmTargetVersion();
        if (projectJavaVersion == null) {
            return false;
        }
        System.out.println("project jvm = " + projectJavaVersion);
        return !JkJavaVersion.ofCurrent().equals(projectJavaVersion);
    }

    private boolean isIdeSupportPresent() {
        return find(ProjectKBean.class).isPresent(); // TODO use IdeSupport instead
    }

    private Path getJekaSrcImlFile() {
        String baseName = getBaseDir().toAbsolutePath().getFileName().toString();
        String name =  baseName + "-jeka.iml";
        return getBaseDir().resolve(".idea").resolve(name);
    }

    // IML containing both jeka-src and project (or IdeSupport)
    private void generateSingleIml(Path imlPath, SdkResolver resolver) {
        JkIml iml = imlGenerator().computeIml2(true, true);
        JkPathFile.of(imlPath)
                .deleteIfExist()
                .createIfNotExist()
                .write(iml.toDoc().toXml().getBytes(StandardCharsets.UTF_8));
        iml.component.setSdk(resolver.computeImlJdk(imlPath, false));
        imlCustomizers.accept(iml);
        JkLog.info("Iml file generated at " + Paths.get("").toAbsolutePath().relativize(imlPath.toAbsolutePath()).normalize());
    }

    private void generateJekaSrcOnlyIml(Path imlFile, SdkResolver resolver, boolean applyCustomizers) {
        JkImlGenerator imlGenerator = imlGenerator()
                .setIdeSupport((JkIdeSupport) null);
        JkIml iml = imlGenerator.computeIml2(true, false);
        iml.setIsModuleBaseJekaSrc(true);
        iml.component.setSdk(resolver.computeImlJdk(imlFile, false));

        // If we use $MODULE_DIR$ instead, jeka methods cannot be run in the ide
        iml.component.getContent().url = "file://$PROJECT_DIR$/jeka-src";
        iml.setIsModuleBaseJekaSrc(false);
        if (applyCustomizers) {
            imlCustomizers.accept(iml);
        }
        JkPathFile.of(imlFile)
                .deleteIfExist()
                .createIfNotExist()
                .write(iml.toDoc().toXml().getBytes(StandardCharsets.UTF_8));
        JkLog.info("Iml file generated at " + Paths.get("").toAbsolutePath().relativize(imlFile).normalize());
    }

    private void generateProjectOnlyIml(Path imlFile, SdkResolver sdkResolver) {
        JkImlGenerator imlGenerator = JkImlGenerator.of();
        imlGenerator
                .setBaseDir(this.getBaseDir())
                .setIdeSupport(() -> IdeSupport.getProjectIde(getRunbase()))
                .setFailOnDepsResolutionError(this.failOnDepsResolutionError)
                .setDownloadSources(this.downloadSources)
                .setUseVarPath(useVarPath);


        JkIml iml = imlGenerator.computeIml2(false, true);
        iml.component.setSdk(sdkResolver.computeImlJdk(imlFile, true));
        iml.setIsModuleBaseJekaSrc(false);
        imlCustomizers.accept(iml);

        JkPathFile.of(imlFile)
                .deleteIfExist()
                .createIfNotExist()
                .write(iml.toDoc().toXml().getBytes(StandardCharsets.UTF_8));
        JkLog.info("Iml file generated at " + Paths.get("").toAbsolutePath().relativize(imlFile).normalize());
    }

    private FocusResult getFocusResult() {
        if (splitModule) {
            if (isIdeSupportPresent()) {
                return FocusResult.get(focus);
            }
            return new FocusResult(true, false);
        }
        if (hasProjectWithDistinctJavaVersion()) {
            JkLog.debug("Distinct java version = true");
            return FocusResult.get(focus);
        }
        return new FocusResult(false, false);
    }

    private static class FocusResult {

        final boolean specificJekaSrcIml;

        final boolean specificProjectIml;

        private FocusResult(boolean specificJekaSrcIml, boolean specificProjectIml) {
            this.specificJekaSrcIml = specificJekaSrcIml;
            this.specificProjectIml = specificProjectIml;
        }

        static FocusResult get(SyncFocus focus) {
            if (focus == null) {
                return new FocusResult(true , true);
            } else if (focus == SyncFocus.JEKA_SRC) {
                return new FocusResult(true , false);
            }
            return new FocusResult(false, true);
        }

        boolean shouldGenerateMergedIml() {
            return !specificJekaSrcIml && !specificProjectIml;
        }

    }

    public Path getProjectRootDir() {
        return projectRootDir(getBaseDir());
    }

    public Path findRegularImlFilePath() {
        Path baseDir = getBaseDir();
        String fileName = baseDir.toAbsolutePath().getFileName().toString() + ".iml";
        if (Files.exists(baseDir.resolve(fileName))) {
            return baseDir.resolve(fileName);
        }
        return baseDir.resolve(".idea/" + fileName);
    }

    // Returns the jeka version mentioned in misc.xml file (project root SDK)
    private JkJavaVersion miscXmlJavaVersion() {
        JkMiscXml miscXml = JkMiscXml.find(getBaseDir());
        if (miscXml == null) {
            return null;
        }
        return miscXml.guessProjectJavaVersion();
    }

    private boolean shouldSkipModulesXml() {
        String skipMProp = getRunbase().getProperties().getTrimmedNonBlank(IntellijKBean.IML_SKIP_MODULE_XML_PROP);
        return "true".equals(skipMProp);
    }

    private SdkResolver sdkResolver() {
        return new SdkResolver(miscXmlJavaVersion());
    }

    private JkModulesXml getModulesXml() {
        return JkModulesXml.of(projectRootDir(getBaseDir()));
    }

    private Path projectRootDir(Path fromDir) {
        if (projectRootDir != null) {
            return projectRootDir;
        }
        if (Files.exists(fromDir.resolve(".idea/misc.xml"))
                || Files.exists(fromDir.resolve(".idea/workspace.xml"))) {
            return fromDir;
        }
        if (fromDir.getParent().getParent() == null) {
            // nothing found, use the current dir
            return Paths.get(".").toAbsolutePath();
        }
        return projectRootDir(fromDir.getParent());
    }

    private class SdkResolver {

        private final JkJavaVersion rootJavaVersion;

        SdkResolver(JkJavaVersion rootJavaVersion) {
            this.rootJavaVersion = rootJavaVersion;
        }

        JkIntellijJdk computeImlJdk(Path imlPath, boolean isForProject) {
            JkIntellijJdk currentImlJkd = JkImlReader.getJdk(imlPath);
            JkLog.debug("Current iml jdk=" + currentImlJkd);
            final JkJavaVersion declaredJavaVersion = jekaModuleJavaVersion(isForProject);
            String javaDistrib = getRunbase().getProperties().getTrimmedNonBlank(JkConstants.JEKA_JAVA_DISTRIB_PROP);
            boolean localJdk = getRunbase().getProperties().getTrimmedNonBlank("jeka.jdk." + declaredJavaVersion)
                    != null;
            if (localJdk) {
                javaDistrib = null;
            } else if (javaDistrib == null) {
                javaDistrib = JkConstants.DEFAULT_JAVA_DISTRIB;
            }
            if (currentImlJkd == null) {
                if (declaredJavaVersion.equals(rootJavaVersion)) {
                    return JkIntellijJdk.ofInherited();
                }
                downloadJkdIfNeeded(javaDistrib, declaredJavaVersion);
                return JkIntellijJdk.ofJekaJdk(javaDistrib, declaredJavaVersion);
            }

            // currentIml is present
            if (currentImlJkd.isJekaManaged() || currentImlJkd.isInherited()) {
                JkLog.debug("Root java version = " + rootJavaVersion);
                if (Objects.equals(rootJavaVersion, declaredJavaVersion)) {
                    return JkIntellijJdk.ofInherited();
                } else {
                    downloadJkdIfNeeded(javaDistrib, declaredJavaVersion);
                    return JkIntellijJdk.ofJekaJdk(javaDistrib, declaredJavaVersion);
                }
            }
            return currentImlJkd;
        }

        private void downloadJkdIfNeeded(String distrib, JkJavaVersion javaVersion) {
            if (distrib == null) {
                return;
            }
            String definedJdk = getRunbase().getProperties().get("jeka.jdk." + javaVersion);
            if (!JkUtilsString.isBlank(definedJdk)) {
                return;
            }
            JkUtilsJdk.getJdk(distrib, javaVersion.toString());
        }

        private JkJavaVersion jekaModuleJavaVersion(boolean isForJkProject) {

            if (isForJkProject) {
                JkIdeSupport ideSupport = IdeSupport.getProjectIde(getRunbase());
                JkJavaVersion javaVersion = ideSupport.getSourceVersion();
                if (javaVersion != null) {
                    return javaVersion;
                }
            }
            String javaVersion = getRunbase().getProperties().get(JkConstants.JEKA_JAVA_VERSION_PROP);
            if (JkUtilsString.isBlank(javaVersion)) {
                return JkJavaVersion.ofCurrent();
            }
            return JkJavaVersion.of(javaVersion);
        }

    }

}