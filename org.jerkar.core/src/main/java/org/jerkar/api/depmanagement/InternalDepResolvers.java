package org.jerkar.api.depmanagement;

class InternalDepResolvers {

    private static final String IVYRESOLVER_CLASS_NAME = IvyResolver.class.getName();

    /**
     * Dependency resolver based on Apache Ivy.
     * This resolver is loaded in a dedicated classloader containing Ivy classes.
     */
    public static InternalDepResolver ivy(JkRepos repos) {
        return IvyClassloader.CLASSLOADER.transClassloaderProxy(
                InternalDepResolver.class, IVYRESOLVER_CLASS_NAME, "of", repos);
    }

}
