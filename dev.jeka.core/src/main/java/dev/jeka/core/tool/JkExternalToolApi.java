package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkRepoFromProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Exported methods to integrate with external tool.
 */
public final class JkExternalToolApi {

    private JkExternalToolApi(){
    }

    public static String getBeanName(String fullyQualifiedClassName) {
        return JkBean.name(fullyQualifiedClassName);
    }

    public static List<String> getCachedBeanClassNames(Path projectRoot) {
        return new EngineBeanClassResolver(projectRoot).readKbeanClasses();
    }

    public static JkRepoSet getDownloadRepos(Path projectDir) {
        Map<String, String> projectProps = PropertyLoader.readProjectPropertiesRecursively(projectDir);
        JkProperties.override(projectProps);
        JkRepoSet result = JkRepoFromProperties.getDownloadRepos();
        JkProperties.removeOverride();
        return result;
    }

    /**
     * Returns <code>true</code> if the specified path is the root directory of a Jeka project.
     */
    public static boolean isJekaProject(Path candidate) {
        Path jekaDir = candidate.resolve(JkConstants.JEKA_DIR);
        if (!Files.isDirectory(jekaDir)) {
            return false;
        }
        Path defDir = jekaDir.resolve(JkConstants.DEF_DIR);
        if (Files.isDirectory(defDir)) {
            return true;
        }
        if (Files.isRegularFile(jekaDir.resolve(JkConstants.PROJECT_PROPERTIES))) {
            return true;
        }
        if (Files.isRegularFile(jekaDir.resolve(JkConstants.CMD_PROPERTIES))) {
            return true;
        }
        if (Files.isRegularFile(jekaDir.resolve("dependencies.txt"))) {
            return true;
        }
        Path wrapperDir = jekaDir.resolve("wrapper");
        if (Files.isDirectory(wrapperDir)) {
            return true;
        }
        if (Files.isRegularFile(candidate.resolve("jekaw"))) {
            return true;
        }
        if ((Files.isRegularFile(candidate.resolve("jekaw.bat")))) {
            return true;
        }
        return false;
    }

    public static Path getImlFile(Path moduleDir) {
        return JkImlGenerator.getImlFilePath(moduleDir);
    }

    public static Map<String, String> getCmdPropertiesContent(Path projectDir) {
        return Environment.projectCmdProperties(projectDir);
    }

}
