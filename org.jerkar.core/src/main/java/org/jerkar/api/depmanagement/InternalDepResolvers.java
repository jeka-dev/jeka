package org.jerkar.api.depmanagement;

class InternalDepResolvers {

    private static final String IVYRESOLVER_CLASS_NAME = IvyResolver.class.getName();

    /**
     * Dependency resolver based on Apache Ivy.
     * This resolver is loaded in a dedicated classloader containing Ivy classes.
     */
    public static ModuleDepResolver ivy(JkRepoSet repos) {
        return IvyClassloader.CLASSLOADER.createTransClassloaderProxy(
                ModuleDepResolver.class, IVYRESOLVER_CLASS_NAME, "of", repos);
    }

}
