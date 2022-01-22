package dev.jeka.core.tool;

/**
 * Exported methods to integrate with external tool
 */
public class JkExternalToolApi {

    public static String getBeanName(String fullyQualifiedClassName) {
        return JkBean.name(fullyQualifiedClassName);
    }

}
