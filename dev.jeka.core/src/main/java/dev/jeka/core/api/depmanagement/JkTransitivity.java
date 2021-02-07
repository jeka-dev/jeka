package dev.jeka.core.api.depmanagement;

/**
 * In Maven repositories, modules are published along a pom.xml metadata containing
 * the transitive dependencies of the module. Here, transitive dependencies can be
 * published with only 2 scopes : either 'compile' nor 'runtime'.<p>
 * This enum specifies how a dependency must take in account its transitive ones.
 */
public enum  JkTransitivity {

    /**
     * Dependency will be fetched without any transitive dependencies
     */
    NONE,

    /**
     * Dependency will be fetch along transitive dependencies declared as 'compile'
     */
    COMPILE,

    /**
     * Dependency will be fetch along transitive dependencies declared as 'runtime'
     * or 'compile'
     */
    RUNTIME;

    public static JkTransitivity ofDeepest(JkTransitivity left, JkTransitivity right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.ordinal() > right.ordinal() ? left : right;
    }
}
