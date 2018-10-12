package org.jerkar.api.depmanagement;

/**
 * Dependency scopes usually used in Java projects
 */
public final class JkJavaDepScopes {

    /**
     * A dependency declared with this scope will be available at compile time but won't be part of the packaged
     * product (similar to Maven scope 'provided').
     */
    public static final JkScope PROVIDED = JkScope.of("provided",
            "Dependencies to compile the project but that should not be embedded in produced artifacts.",
            false);

    /**
     * A dependency resolution made with this scope will only fetch dependencies declared with {@link #COMPILE} scope and
     * transitive dependencies declared with {@link #COMPILE } scope as well. <p>>
     *
     * <b>CAUTION :</b> When resolving {@link #RUNTIME} dependencies, transitive 'runtime' dependencies won't be fetched if
     * it's coming to a 'compile' one. <b/>
     * In such it differs from Maven 'compile' scope (resolving 'runtime' in Maven will fetch transitive 'runtime' dependencies coming from 'compile' ones).<b/>
     * If you want to have a dependency scope equivalent to Maven 'compile', you need to declare dependencies with
     * two scopes : {@link #COMPILE} and {@link #RUNTIME} or their shorthand {@link #COMPILE_AND_RUNTIME}.
     */
    public static final JkScope COMPILE = JkScope.of("compile",
            "Dependencies to compile the project.", true);

    /**
     * A dependency declared with this scope will be present in the classpath for packaging or running the module.<b/>
     * If it is a library, dependencies will be included in the fat jar.<b/>
     * If it is a war, dependencies will be included in war file. <b/>
     * If it is a main application, dependencies will be part of the runtime classpath.<p>
     *
     * A dependency resolution made with this scope will fetch dependencies declared with {@link #COMPILE} or {@link #RUNTIME}
     * plus their transitive dependencies declared with {@link #COMPILE } or {@link #RUNTIME}.
     */
    public static final JkScope RUNTIME = JkScope.of("runtime",
            "Dependencies to compile the project but that should not be embedded in produced artifacts.",
            true, COMPILE);

    /**
     * A dependency declared with this scope will be present in testing classpath only.
     *
     * A dependency resolution made with this scope will fetch dependencies declared with {@link #COMPILE}, {@link #RUNTIME} or  {@link #TEST}
     * plus their transitive dependencies declared with {@link #COMPILE }, {@link #RUNTIME} or {@link #TEST}.
     */
    public static final JkScope TEST = JkScope.of("test",
            "Dependencies necessary to compile and run tests.",
            true,
            RUNTIME, PROVIDED);

    /** This scope is used for publication purpose */
    public static final JkScope SOURCES = JkScope.of("sources",
            "Contains the source artifacts.",
            false);

    /** This scope is used for publication purpose */
    public static final JkScope JAVADOC = JkScope.of("javadoc",
            "Contains the javadoc of this project",
            false);

    public static JkScope of(String name) {
        if (name.equalsIgnoreCase("compile")) {
            return COMPILE;
        }
        if (name.equalsIgnoreCase("runtime")) {
            return RUNTIME;
        }
        if (name.equalsIgnoreCase("test")) {
            return TEST;
        }
        if (name.equalsIgnoreCase("provided")) {
            return PROVIDED;
        }
        return null;
    }

    /**
     * Shorthand to declare both COMPILE and RUNTIME scope at once. This is the default scope for dependencies.
     * It is equivalent to Maven 'compile'.
     */
    public static final JkScope[] COMPILE_AND_RUNTIME = new JkScope[] {COMPILE, RUNTIME};

    private static final String ARCHIVE_MASTER = "archives(master)";

    /**
     * Scope mapping used
     */
    public static final JkScopeMapping DEFAULT_SCOPE_MAPPING = JkScopeMapping
            .of(COMPILE).to(ARCHIVE_MASTER, COMPILE.getName() + "(default)")
            .and(PROVIDED).to(ARCHIVE_MASTER, COMPILE.getName() + "(default)")
            .and(RUNTIME).to(ARCHIVE_MASTER, RUNTIME.getName() + "(default)")
            .and(TEST).to(ARCHIVE_MASTER, RUNTIME.getName() + "(default)", TEST.getName() + "(default)");


    /**
     * Scopes necessary for compiling production code.
     */
    public static final JkScope[] SCOPES_FOR_COMPILATION = new JkScope[] {COMPILE, PROVIDED};

    /**
     * Scopes necessary for both compiling tests and run them.
     */
    public static final JkScope[] SCOPES_FOR_TEST = new JkScope[] {TEST, PROVIDED};


}
