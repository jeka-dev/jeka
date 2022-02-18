package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkRepoFromProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.system.JkProperties;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Exported methods to integrate with external tool
 */
public class JkExternalToolApi {

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

}
