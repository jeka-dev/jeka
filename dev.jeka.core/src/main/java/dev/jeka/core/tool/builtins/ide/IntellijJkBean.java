package dev.jeka.core.tool.builtins.ide;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator2;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.Main;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

@JkDoc("Generates Idea Intellij metadata files (*.iml and modules.xml).")
public final class IntellijJkBean extends JkBean {

    @JkDoc("If true, dependency paths will be expressed relatively to $JEKA_REPO$ and $JEKA_HOME$ path variable instead of absolute paths.")
    public boolean useVarPath = true;

    @JkDoc("If true, the iml generation fails when a dependency can not be resolved. If false, it will be ignored " +
            "(only a warning will be notified).")
    public boolean failOnDepsResolutionError = true;

    @JkDoc("If specified, the specified module will be added as dependency in place of tJeka lib. Can be set to empty string for skipping Jeka dependency.")
    public String jekaModule;

    private LinkedHashSet<String> projectLibraries = new LinkedHashSet<>();

    private Consumer<JkImlGenerator2> imlGeneratorConfigurer = jkImlGenerator2 -> {};

    public void configureImlGenerator(Consumer<JkImlGenerator2> imlGeneratorConfigurer) {
        this.imlGeneratorConfigurer = this.imlGeneratorConfigurer.andThen(imlGeneratorConfigurer);
    }

    @JkDoc("Generates IntelliJ [my-module].iml file.")
    public void iml() {
        Path basePath = getBaseDir();
        JkImlGenerator2 imlGenerator = imlGenerator2();
        imlGeneratorConfigurer.accept(imlGenerator);
        JkIml iml = imlGenerator.computeIml();
        final JkPathFile imlFile = JkPathFile.of(findImlFile(basePath))
                .deleteIfExist()
                .createIfNotExist()
                .write(iml.toDoc().toXml().getBytes(StandardCharsets.UTF_8));
        JkLog.info("Iml file generated at " + imlFile.get());
    }

    private static Path findImlFile(Path dir) {
        return JkImlGenerator2.findImlFile(dir).orElse(dir.resolve(".idea")
                .resolve(dir.getFileName().toString() + ".iml"));
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
        JkPathTree.of(getBaseDir())
                .andMatching(true, "**/" + JkConstants.DEF_DIR, JkConstants.DEF_DIR)
                .andMatching(false, "**/" + JkConstants.OUTPUT_PATH + "/**")
                .stream()
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

    private JkImlGenerator2 imlGenerator2() {
        JkImlGenerator2 imlGenerator = JkImlGenerator2.of()
                .setBaseDir(this.getBaseDir())
                .setDefClasspath(this.getRuntime().getClasspath())
                .setDefImportedProjects(this.getRuntime().getImportedProjects())
                .setIdeSupport(IdeSupport.getProjectIde(this))
                .setFailOnDepsResolutionError(this.failOnDepsResolutionError)
                .setUseVarPath(useVarPath);
        if (jekaModule != null) {
            imlGenerator.setSkipJeka(true);
            if (!jekaModule.trim().isEmpty()) {
                imlGenerator.configureIml(jkIml -> jkIml.getComponent().addModuleOrderEntry(jekaModule, JkIml.Scope.TEST));
            }
        }
        return imlGenerator;
    }

}