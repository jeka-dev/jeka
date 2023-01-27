package dev.jeka.core.tool.builtins.ide;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
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
public final class IntellijJkBean extends JkBean {

    @JkDoc("Add a iml dependency on the specified module (hosting Jeka) instead of adding a direct dependency on Jeka jar." +
            "This is desirable on multi-module projects to share a single Jeka version.")
    public String jekaModuleName;

    @JkDoc("If true, dependency paths will be expressed relatively to $JEKA_REPO$ and $JEKA_HOME$ path variable instead of absolute paths.")
    public boolean useVarPath = true;

    @JkDoc("If true, the iml generation fails when a dependency can not be resolved. If false, it will be ignored " +
            "(only a warning will be notified).")
    public boolean failOnDepsResolutionError = true;

    @JkDoc("The path where iml file must be generated. If null, Jeka will decide for a proper place. Mostly used by external tools.")
    public Path imlFile;

    private LinkedHashSet<String> projectLibraries = new LinkedHashSet<>();

    private Consumer<JkImlGenerator> imlGeneratorConfigurer = jkImlGenerator2 -> {};

    public IntellijJkBean configureImlGenerator(Consumer<JkImlGenerator> imlGeneratorConfigurer) {
        this.imlGeneratorConfigurer = this.imlGeneratorConfigurer.andThen(imlGeneratorConfigurer);
        return this;
    }

    public IntellijJkBean configureIml(Consumer<JkIml> imlConfigurer) {
        configureImlGenerator(imlGeneratorConfigurer -> imlGeneratorConfigurer.configureIml(imlConfigurer));
        return this;
    }

    /**
     * In multi-module project, it's desirable that a unique module holds the dependency on Jeka, so it can be
     * updated in a central location.
     * Calling this method will force the generated iml to have a dependency on the specified module instead
     * of on the jeka-core jar.
     */
    public IntellijJkBean useJekaDefinedInModule(String intellijModule) {
        configureImlGenerator(imlGenerator -> imlGenerator.setExcludeJekaLib(true));
        return configureIml(iml -> iml.component.addModuleOrderEntry(intellijModule, JkIml.Scope.TEST));
    }

    /**
     * In multi-module project, Jeka dependency may be already hold by a module this one depends on.
     * Calling this method prevents to add a direct Jeka dependency on this module.
     */
    public IntellijJkBean excludeJekaLib() {
        return configureImlGenerator(imlGenerator -> imlGenerator.setExcludeJekaLib(true));
    }

    @JkDoc("Generates IntelliJ [my-module].iml file.")
    public void iml() {
        Path basePath = getBaseDir();
        JkImlGenerator imlGenerator = imlGenerator();
        if (!JkUtilsString.isBlank(this.jekaModuleName)) {
            useJekaDefinedInModule(this.jekaModuleName.trim());
        }
        imlGeneratorConfigurer.accept(imlGenerator);
        JkIml iml = imlGenerator.computeIml();
        Path imlPath = Optional.ofNullable(this.imlFile).orElse(JkImlGenerator.getImlFilePath(basePath));
        JkPathFile.of(imlPath)
                .deleteIfExist()
                .createIfNotExist()
                .write(iml.toDoc().toXml().getBytes(StandardCharsets.UTF_8));
        JkLog.info("Iml file generated at " + imlPath);
    }


    /** Generate modules.xml files */
    @JkDoc("Generates ./idea/modules.xml file.")
    public void modulesXml() {
        checkProjectRoot();
        final Path current = getBaseDir();
        final Iterable<Path> imls = JkPathTree.of(getBaseDir()).andMatching(true,"**.iml").getFiles();
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

    private void generateImlExec(Path moduleDir) {
        JkLog.startTask("Generate iml file on '%s'", moduleDir);
        Main.exec(moduleDir, "intellij#iml", "-dci");
        JkLog.endTask();
    }

    public IntellijJkBean addProjectLibrary(String xml) {
        this.projectLibraries.add(xml);
        return this;
    }

    @JkDoc("Shorthand for intellij#allIml + intellij#modulesXml.")
    public void fullProject() {
        checkProjectRoot();
        allIml();
        modulesXml();
    }

    private void checkProjectRoot() {
        final IntellijModulesXmlGenerator intellijModulesXmlGenerator = new IntellijModulesXmlGenerator(getBaseDir(),
                Collections.emptyList());
        JkUtilsAssert.state(Files.exists(intellijModulesXmlGenerator.outputFile()),
                "Folder where Jeka has run '%s' seems not to be the root of the project cause no file %s " +
                        "has been found here.\nPlease relaunch this command from IntelliJ project root directory."
                , getBaseDir().toAbsolutePath(), intellijModulesXmlGenerator.outputFile());
    }

    private JkImlGenerator imlGenerator() {
        JkImlGenerator imlGenerator = JkImlGenerator.of()
                .setBaseDir(this.getBaseDir())
                .setDefClasspath(this.getRuntime().getClasspath())
                .setDefImportedProjects(this.getRuntime().getImportedProjects())
                .setIdeSupport(IdeSupport.getProjectIde(this))
                .setFailOnDepsResolutionError(this.failOnDepsResolutionError)
                .setUseVarPath(useVarPath);
        return imlGenerator;
    }

}