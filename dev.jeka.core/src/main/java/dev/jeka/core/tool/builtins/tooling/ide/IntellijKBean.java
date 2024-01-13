package dev.jeka.core.tool.builtins.tooling.ide;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.Main;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

@JkDoc("Generates Idea Intellij metadata files (*.iml and modules.xml).")
public final class IntellijKBean extends KBean {

    @JkDoc("Add a iml dependency on the specified module (hosting Jeka) instead of adding a direct dependency on Jeka jar." +
            "This is desirable on multi-module projects to share a single Jeka version.")
    private String jekaModuleName;

    @JkDoc("If true, dependency paths will be expressed relatively to $JEKA_REPO$ and $JEKA_HOME$ path variable instead of absolute paths.")
    private final boolean useVarPath = true;

    @JkDoc("If true, the iml generation fails when a dependency can not be resolved. If false, it will be ignored " +
            "(only a warning will be notified).")
    private final boolean failOnDepsResolutionError = true;

    @JkDoc("When true, .iml file will be generated assuming that 'jeka'' folder is the root of a specific module. " +
            "This is useful for working with tool as Maven or Gradle that manage the intellij dependencies by their own.")
    private boolean dedicatedJekaModule;

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
    public final JkImlGenerator imlGenerator = JkImlGenerator.of();

    private final LinkedHashSet<String> projectLibraries = new LinkedHashSet<>();

    @Override
    protected void init() {
        imlGenerator
                .setBaseDir(this.getBaseDir())
                .setDefClasspath(this.getRuntime().getClasspath())
                .setDefImportedProjects(this.getRuntime().getImportedProjects())
                .setIdeSupport(() -> IdeSupport.getProjectIde(getRuntime()))
                .setFailOnDepsResolutionError(this.failOnDepsResolutionError)
                .setDedicatedJekaModule(this.dedicatedJekaModule)
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
        Path basePath = getBaseDir();
        JkIml iml = imlGenerator.computeIml();
        Path imlPath = Optional.ofNullable(imlFile).orElse(JkImlGenerator.getImlFilePath(basePath,
                dedicatedJekaModule));
        JkPathFile.of(imlPath)
                .deleteIfExist()
                .createIfNotExist()
                .write(iml.toDoc().toXml().getBytes(StandardCharsets.UTF_8));
        JkLog.info("Iml file generated at " + imlPath);
    }

    /**
     * Generate modules.xml files
     */
    @JkDoc("Generates ./idea/modules.xml file.")
    public void modulesXml() {
        checkProjectRoot();
        final Path current = getBaseDir();
        final Iterable<Path> imls = JkPathTree.of(getBaseDir()).andMatching(true, "**.iml").getFiles();
        final IntellijModulesXmlGenerator intellijModulesXmlGenerator = new IntellijModulesXmlGenerator(current, imls);
        intellijModulesXmlGenerator.generate();
        JkLog.info("File generated at : " + intellijModulesXmlGenerator.outputFile());
    }

    @JkDoc("Generates iml files on this folder and its descendant recursively.")
    public void allIml() {
        Stream<Path> stream = JkPathTree.of(getBaseDir())
                .andMatching(true, "**/" + JkConstants.DEF_DIR, JkConstants.DEF_DIR)
                .andMatching(false, "**/" + JkConstants.OUTPUT_PATH + "/**")
                .stream();
        stream
                .distinct()
                .map(path -> path.getParent().getParent())
                .map(path -> path == null ? getBaseDir() : path)
                .forEach(this::generateImlExec);
    }

    @JkDoc("Shorthand for intellij#allIml + intellij#modulesXml.")
    public void fullProject() {
        checkProjectRoot();
        allIml();
        modulesXml();
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
        Main.exec(moduleDir, "intellij#iml", "-dci");
        JkLog.endTask();
    }

    private void checkProjectRoot() {
        final IntellijModulesXmlGenerator intellijModulesXmlGenerator = new IntellijModulesXmlGenerator(getBaseDir(),
                Collections.emptyList());
        JkUtilsAssert.state(Files.exists(intellijModulesXmlGenerator.outputFile()),
                "Folder where Jeka has run '%s' seems not to be the root of the project cause no file %s " +
                        "has been found here.\nPlease relaunch this command from IntelliJ project root directory."
                , getBaseDir().toAbsolutePath(), intellijModulesXmlGenerator.outputFile());
    }

}