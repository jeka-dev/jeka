package dev.jeka.core.tool;

import java.nio.file.Path;
import java.util.List;

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

}
