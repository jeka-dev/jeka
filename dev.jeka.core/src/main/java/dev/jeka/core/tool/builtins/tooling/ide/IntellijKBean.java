package dev.jeka.core.tool.builtins.tooling.ide;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.MainLegacy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.function.Consumer;

@JkDoc("Manages Intellij metadata files")
public final class IntellijKBean extends KBean {

    // Flag for skipping modules.xml creation when testing.
    public static final String IML_SKIP_MODULE_XML_PROP = "jeka.intellij.iml.skip.moduleXml";

    @JkDoc("Add a iml dependency on the specified module (hosting Jeka) instead of adding a direct dependency on Jeka jar." +
            "This is desirable on multi-module projects to share a single Jeka version.")
    private String jekaModuleName;

    @JkDoc("If true, dependency paths will be expressed relatively to $JEKA_REPO$ and $JEKA_HOME$ path variable instead of absolute paths.")
    private final boolean useVarPath = true;

    @JkDoc("If true, the iml generation fails when a dependency can not be resolved. If false, it will be ignored " +
            "(only a warning will be notified).")
    private final boolean failOnDepsResolutionError = true;

    @JkDoc("The path where iml file must be generated. If null, Jeka will decide for a proper place. Mostly used by external tools.")
    public Path imlFile;

    @JkDoc("If mentioned, and jdkName is null, the generated iml will specify this jdkName")
    private String suggestedJdkName;

    @JkDoc("If mentioned, the generated iml will specify this jdkName")
    private String jdkName;

    /**
     * Underlying imlGenerator used to generate Iml. <p>
     * Use this object to  configure finely generated iml.
     */
    @JkDoc(hide = true)
    public final JkImlGenerator imlGenerator = JkImlGenerator.of();

    private final LinkedHashSet<String> projectLibraries = new LinkedHashSet<>();

    @Override
    protected void init() {
        imlGenerator
                .setBaseDir(this.getBaseDir())
                .setJekaSrcClasspath(this.getRunbase().getClasspath())
                .setJekaSrcImportedProjects(this.getRunbase().getImportedRunbaseDirs())
                .setIdeSupport(() -> IdeSupport.getProjectIde(getRunbase()))
                .setFailOnDepsResolutionError(this.failOnDepsResolutionError)
                .setUseVarPath(useVarPath);
        if (!JkUtilsString.isBlank(jekaModuleName)) {
            useJekaDefinedInModule(jekaModuleName.trim());
        }
        if (!JkUtilsString.isBlank(jdkName)) {
            imlGenerator.configureIml(iml -> iml.component.setJdkName(jdkName));
        } else if (!JkUtilsString.isBlank(suggestedJdkName)) {
            imlGenerator.configureIml(iml -> iml.component.setJdkName(suggestedJdkName));
        }
    }

    @JkDoc("Generates IntelliJ [my-module].iml file.")
    public void iml() {
        JkIml iml = imlGenerator.computeIml();
        Path imlPath = getImlFile();
        JkPathFile.of(imlPath)
                .deleteIfExist()
                .createIfNotExist()
                .write(iml.toDoc().toXml().getBytes(StandardCharsets.UTF_8));
        JkLog.info("Iml file generated at " + getBaseDir().relativize(imlPath));
        if ("true".equals(getRunbase().getProperties().get(IML_SKIP_MODULE_XML_PROP))) {
            return;
        }
        IntelliJProject intelliJProject = IntelliJProject.find(getBaseDir());
        if (!Files.exists(intelliJProject.getModulesXmlPath())) {
            intelliJProject.generateModulesXml(imlPath);
            JkLog.info("%s generated.", intelliJProject.getModulesXmlPath());
        }
    }

    /**
     * Generate modules.xml files
     */
    @JkDoc("Generates ./idea/modules.xml file by grabbing all .iml files presents " +
            "in root or sub-directory of the project.")
    public void modulesXml() {
        IntelliJProject intelliJProject = IntelliJProject.find(getBaseDir());
        intelliJProject.regenerateModulesXml();
        JkLog.info("File generated at : " + intelliJProject.getModulesXmlPath());
    }

    @JkDoc("Generates iml files on this folder and its descendant recursively.")
    public void allIml() {
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

    @JkDoc("Try to fix project by deleting workspace.xml and touching iml file")
    public void fixProject() {
        iml();
        IntelliJProject.find(getBaseDir()).deleteWorkspaceXml();
        modulesXml();
        iml();;
    }

    /**
     * Configures IML file that will be generated.
     */
    public IntellijKBean configureIml(Consumer<JkIml> imlConfigurator) {
        this.imlGenerator.configureIml(imlConfigurator);
        return this;
    }

    /**
     * In multi-module project, it's desirable that a unique module holds the dependency on Jeka, so it can be
     * updated in a central location.
     * Calling this method will force the generated iml to have a dependency on the specified module instead
     * of on the jeka-core jar.
     */
    public IntellijKBean useJekaDefinedInModule(String intellijModule) {
        imlGenerator.setExcludeJekaLib(true);
        return configureIml(iml -> iml.component.addModuleOrderEntry(intellijModule, JkIml.Scope.TEST));
    }

    /**
     * In multi-module project, Jeka dependency may be already hold by a module this one depends on.
     * Calling this method prevents to add a direct Jeka dependency on this module.
     */
    public IntellijKBean excludeJekaLib() {
        imlGenerator.setExcludeJekaLib(true);
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

    private void generateImlExec(Path moduleDir) {
        JkLog.startTask("Generate iml file on '%s'", moduleDir);
        MainLegacy.exec(moduleDir, "intellij#iml", "-dci");
        JkLog.endTask();
    }

    private Path getImlFile() {
        return Optional.ofNullable(imlFile).orElse(JkImlGenerator.getImlFilePath(getBaseDir()));
    }



}