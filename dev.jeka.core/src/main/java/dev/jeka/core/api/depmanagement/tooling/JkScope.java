package dev.jeka.core.api.depmanagement.tooling;

import dev.jeka.core.api.utils.JkUtilsString;

/**
 * Defines a context where is defined dependencies of a given project. According
 * we need to compile, test or run the application, the dependencies may
 * diverge. For example, <code>Junit</code> library may only be necessary for
 * testing, so we can declare that
 * <code>Junit</scope> is only necessary for scope <code>TEST</code>.
 * <p>
 * Similar to Maven <code>scope</code> or Ivy <code>configuration</code>.
 *
 * @author Jerome Angibaud
 */
public final class JkScope {

    /**
     * A scope meant to tag a dependency as necessary for compiling.<p>
     *
     * Default scopeMapping :a dependency resolution made with this scope will fetch the module along its dependencies
     * declared as {@link #COMPILE}.
     *
     * <b>CAUTION :</b> Using default scope mapping, when resolving {@link #RUNTIME} dependencies,
     * transitive 'runtime' dependencies won't be fetched if
     * it's coming to a 'compile' one. <b/>
     * In such, it differs from Maven 'compile' scope : resolving 'runtime' in Maven will fetch transitive 'runtime'
     * dependencies coming from 'compile' ones.<b/>
     * If you want to have a dependency scope equivalent to Maven 'compile', you need to declare dependencies with
     * two scopes : {@link #COMPILE} and {@link #RUNTIME} or their shorthand {@link #COMPILE_AND_RUNTIME}.
     */
    public static final JkScope COMPILE = of("compile", "Required for compilation", true);

    /**
     * A dependency declared with this scope will be present in the classpath for packaging or running the module.<b/>
     * If it is a library, dependencies will be included in the fat jar.<b/>
     * If it is a war, dependencies will be included in war file. <b/>
     * If it is a main application, dependencies will be part of the runtime classpath.<p>
     *
     * A dependency resolution made with this scope will fetch dependencies declared with {@link #COMPILE} or {@link #RUNTIME}
     * plus their transitive dependencies declared with {@link #COMPILE } or {@link #RUNTIME}.
     */
    public static final JkScope RUNTIME = of("runtime",
            "Dependencies to run the project but not needed to compile it.",
            true);

    /**
     * A dependency declared with this scope will be available at compile time but won't be part of the packaged
     * product (similar to Maven scope 'provided').
     */
    public static final JkScope PROVIDED = of("provided",
            "Dependencies to compile the project but that should not be embedded in produced artifacts.",
            false);

    /**
     * A dependency declared with this scope will be present in testing classpath only.
     *
     * A dependency resolution made with this scope will fetch dependencies declared with {@link #COMPILE}, {@link #RUNTIME} or  {@link #TEST}
     * plus their transitive dependencies declared with {@link #COMPILE }, {@link #RUNTIME} or {@link #TEST}.
     */
    public static final JkScope TEST = of("test",
            "Dependencies necessary to compile and run tests.",
            true);



    /**
     * Shorthand to declare both COMPILE and RUNTIME scope at once.
     * It is equivalent to Maven 'compile'.
     */
    public static final JkScope[] COMPILE_AND_RUNTIME = new JkScope[] {COMPILE, RUNTIME};

    /** This scope is used for publication purpose */
    public static final JkScope SOURCES = of("sources",
            "Contains the source artifacts.",
            false);

    /** This scope is used for publication purpose */
    public static final JkScope JAVADOC = of("javadoc",
            "Contains the javadoc of this project",
            false);

    /**
     * Creates a new {@link JkScope} passing its name.
     */
    public static JkScope of(String name) {
        return new JkScope(name,  "", true);
    }

    private final String name;

    private final String description;

    private final boolean transitive;

    private JkScope(String name, String description,
                    boolean transitive) {
        super();
        final String illegal = JkUtilsString.firstMatching(name, ",", "->");
        if (illegal != null) {
            throw new IllegalArgumentException("Scope name can't contain '" + illegal + "'");
        }
        this.name = name;
        this.description = description;
        this.transitive = transitive;
    }

    public static JkScope of(String name, String description, boolean transitive) {
        return new JkScope(name, description, transitive);
    }

    /**
     * Returns the name of this scope. Name is used as identifier for scopes.
     */
    public String getName() {
        return name;
    }

    /**
     * Human description for the purpose of this scope, can be <code>null</code>.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns <code>true</code> if the dependencies defined with this scope should be resolved recursively
     * (meaning returning the dependencies of the dependencies and so on)
     */
    public boolean isTransitive() {
        return this.transitive;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JkScope other = (JkScope) obj;
        if (name == null) {
            return other.name == null;
        } else return name.equals(other.name);
    }

    @Override
    public String toString() {
        return name;
    }

}
