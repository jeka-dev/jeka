package dev.jeka.core.tool.builtins.ide;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkFileSystemDependency;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinModules;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.Main;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@JkDoc("Generates Idea Intellij metadata files (*.iml and modules.xml).")
public final class IntellijJkBean extends JkBean {

    @JkDoc("If true, dependency paths will be expressed relatively to $JEKA_REPO$ and $JEKA_HOME$ path variable instead of absolute paths.")
    public boolean useVarPath = true;

    @JkDoc("By default, generated iml files specify a JDK inherited from the project setup. " +
            "Set this option to 'true' for  forcing the JDK version to the one defined in JkProject.")
    public boolean forceJdkVersion = false;

    @JkDoc("If true, the iml generation fails when a dependency can not be resolved. If false, it will be ignored " +
            "(only a warning will be notified).")
    public boolean failOnDepsResolutionError = true;

    @JkDoc("If true, dependency to Jeka jar will be excluded (assuming it will be got from a module dependencies")
    public boolean imlSkipJeka;

    @JkDoc("Comma separated modules to be added as dependencies for jeka classpath")
    public String imlJekaExtraModules;

    @JkDoc("Override JEKA_HOME environment variable")
    public Path jekaHome;

    private LinkedHashSet<String> projectLibraries = new LinkedHashSet<>();

    /** Generates Idea [my-module].iml file */
    @JkDoc("Generates Idea [my-module].iml file.")
    public void iml() {
        final JkImlGenerator generator;
        JkIdeSupport projectIde = IdeSupport.getProjectIde(this);
        if (projectIde != null) {
            generator = JkImlGenerator.of(projectIde);
        } else {
            generator = JkImlGenerator.of(JkIdeSupport.of(getBaseDir()));
        }
        generator.setFailOnDepsResolutionError(failOnDepsResolutionError);
        generator.setUseVarPath(useVarPath);
        generator.setExplicitJekaHome(jekaHome);
        JkDependencySet defDependencies = IdeSupport.classpathAsDependencySet();
        if (imlSkipJeka) {
            defDependencies = defDependencies
                    .minus(JkFileSystemDependency.of(JkLocator.getJekaJarPath().getFileName()));
        }

        // Add Kotlin runtime lib if any kotlin source file found in def directory
        Path def = getBaseDir().resolve(JkConstants.DEF_DIR);
        if (JkPathTree.of(def).andMatching("**.kt", "*.kt").count(1, false) > 0) {
            JkKotlinCompiler kotlinCompiler = JkKotlinCompiler.ofJvm(getRuntime().getDependencyResolver().getRepos());
            if (kotlinCompiler.isProvidedCompiler()) {
                defDependencies = defDependencies.andFiles(kotlinCompiler.getStdLib());
            } else {
                defDependencies = defDependencies.and(JkKotlinModules.STDLIB_JDK8 + ":" + kotlinCompiler.getVersion());
            }
        }

        generator.setDefDependencies(defDependencies);
        generator.setDefDependencyResolver(getRuntime().getDependencyResolver());
        List<String> importedProjectModuleDeps = new LinkedList<>();
        if (!JkUtilsString.isBlank(this.imlJekaExtraModules)) {
            for (String module : JkUtilsString.splitTrimmed(this.imlJekaExtraModules, ",")) {
                importedProjectModuleDeps.add(module);
            }
        }
        getImportedJkBeans().get(false).stream()
                .map(JkBean::getBaseDir)
                .distinct()
                .map(path -> path.getFileName().toString())
                .forEach(importedProjectModuleDeps::add);
        generator.setExtraJekaModules(importedProjectModuleDeps);
        Path basePath = getBaseDir();
        if (getRuntime().getBeanOptional(ProjectJkBean.class).isPresent()) {
            getRuntime().getBean(ProjectJkBean.class);
            generator.setForceJdkVersion(forceJdkVersion);
        }


        this.projectLibraries.forEach(libraryName -> generator.addProjectLibrary(libraryName));
        final String xml = generator.generate();
        final Path imlFile = findIml(basePath);
        JkUtilsPath.deleteIfExists(imlFile);
        JkUtilsPath.createDirectories(imlFile.toAbsolutePath().normalize().getParent());
        JkUtilsPath.write(imlFile, xml.getBytes(Charset.forName("UTF-8")));
        JkLog.info("Iml file generated at " + imlFile);
    }

    private static Path findIml(Path dir) {
        String dirNameCandidate = dir.getFileName().toString() + ".iml";
        Path dirNameCandidatePath = dir.resolve(dirNameCandidate);
        if (Files.exists(dirNameCandidatePath)) {
            return dirNameCandidatePath;
        }
        Path ideaDir = dir.resolve(".idea");
        Path ideaDirNameCandidatePath = ideaDir.resolve(dirNameCandidate);
        if (Files.exists(ideaDirNameCandidatePath)) {
            return ideaDirNameCandidatePath;
        }
        Path firstInDir = JkUtilsPath.listDirectChildren(dir).stream()
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".iml"))
                .findFirst().orElse(null);
        if (firstInDir != null) {
            return firstInDir;
        }
        if (Files.exists(ideaDir) && Files.isDirectory(ideaDir)) {
            Path firstInIdea = JkUtilsPath.listDirectChildren(dir.resolve(".idea")).stream()
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".iml"))
                    .findFirst().orElse(null);
            if (firstInIdea != null) {
                return firstInIdea;
            }
        }
        return dirNameCandidatePath;
    }

    /** Generate modules.xml files */
    @JkDoc("Generates ./idea/modules.xml file.")
    public void modulesXml() {
        final Path current = getBaseDir();
        final Iterable<Path> imls = JkPathTree.of(getBaseDir()).andMatching(true,"**.iml").getFiles();
        final IntellijModulesXmlGenerator intellijModulesXmlGenerator = new IntellijModulesXmlGenerator(current, imls);
        intellijModulesXmlGenerator.generate();
        JkLog.info("File generated at : " + intellijModulesXmlGenerator.outputFile());
    }

    @JkDoc("Generates iml files on this folder and its descendant recursively.")
    public void allIml() {
        final Iterable<Path> folders = JkPathTree.of(getBaseDir())
                .andMatching(true, "**/" + JkConstants.DEF_DIR, JkConstants.DEF_DIR)
                .andMatching(false, "**/" + JkConstants.OUTPUT_PATH + "/**")
                .stream().collect(Collectors.toList());
        for (final Path folder : folders) {
            final Path projectFolder = folder.getParent().getParent();
            JkLog.startTask("Generate iml file on " + projectFolder);
            try {
                Main.exec(projectFolder, "intellij#iml");
            } catch (Exception e) {
                JkLog.warn("Generating Iml failed : Try to generate it using -CC=JkClass option. Failure cause : ");
                JkLog.warn(e.getMessage());
                PrintWriter printWriter = new PrintWriter(JkLog.getErrPrintStream());
                e.printStackTrace(printWriter);
                printWriter.flush();
                try {
                    Main.exec(projectFolder, "intellij#iml", "-CC=JkClass");
                } catch (Exception e1) {
                    JkLog.warn("Generating Iml file failed;");
                }
            }
            JkLog.endTask();
        }
    }

    public IntellijJkBean addProjectLibrary(String xml) {
        this.projectLibraries.add(xml);
        return this;
    }

    @JkDoc("Shorthand for intellij#allIml + intellij#modulesXml.")
    public void all() {
        allIml();
        modulesXml();
    }

}