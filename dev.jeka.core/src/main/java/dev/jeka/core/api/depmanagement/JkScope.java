package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.*;

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
     * A dependency resolution made with this scope will only fetch dependencies declared with {@link #COMPILE} scope and
     * transitive dependencies declared with {@link #COMPILE } scope as well. <p>>
     *
     * <b>CAUTION :</b> Using default scope mapping, when resolving {@link #RUNTIME} dependencies, transitive 'runtime' dependencies won't be fetched if
     * it's coming to a 'compile' one. <b/>
     * In such, it differs from Maven 'compile' scope : resolving 'runtime' in Maven will fetch transitive 'runtime' dependencies coming from 'compile' ones.<b/>
     * If you want to have a dependency scope equivalent to Maven 'compile', you need to declare dependencies with
     * two scopes : {@link #COMPILE} and {@link #RUNTIME} or their shorthand {@link #COMPILE_AND_RUNTIME}.
     */
    public static final JkScope COMPILE = of("compile",
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
    public static final JkScope RUNTIME = of("runtime",
            "Dependencies to run the project but n,ot needed to compile it.",
            true, COMPILE);

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
            true,
            RUNTIME, PROVIDED);

    /**
     * Shorthand to declare both COMPILE and RUNTIME scope at once. This is the default scope for dependencies.
     * It is equivalent to Maven 'compile'.
     */
    public static final JkScope[] COMPILE_AND_RUNTIME = new JkScope[] {COMPILE, RUNTIME};

    /**
     * Scopes necessary for compiling production code.
     */
    public static final JkScope[] SCOPES_FOR_COMPILATION = new JkScope[] {COMPILE, PROVIDED};

    /**
     * Scopes necessary for both compiling tests and run them.
     */
    public static final JkScope[] SCOPES_FOR_TEST = new JkScope[] {TEST, PROVIDED};

    /** This scope is used for publication purpose */
    public static final JkScope SOURCES = of("sources",
            "Contains the source artifacts.",
            false);

    /** This scope is used for publication purpose */
    public static final JkScope JAVADOC = of("javadoc",
            "Contains the javadoc of this project",
            false);

    /**
     * Useful when using scope mapping. As documented in Ivy, it stands for the main archive.
     */
    public static final String ARCHIVE_MASTER = "archives(master)";

    /**
     * Scope mapping used
     */
    public static final JkScopeMapping DEFAULT_SCOPE_MAPPING = JkScopeMapping
            .of(COMPILE).to(ARCHIVE_MASTER, COMPILE.getName() + "(default)")
            .and(PROVIDED).to(ARCHIVE_MASTER, COMPILE.getName() + "(default)")
            .and(RUNTIME).to(ARCHIVE_MASTER, RUNTIME.getName() + "(default)")
            .and(TEST).to(ARCHIVE_MASTER, RUNTIME.getName() + "(default)");

    /**
     * Creates a new {@link JkScope} passing its name.
     */
    public static JkScope of(String name) {
        return new JkScope(name, new HashSet<>(), "", true);
    }

    private final Set<JkScope> extendedScopes;

    private final String name;

    private final String description;

    private final boolean transitive;

    private JkScope(String name, Set<JkScope> extendedScopes, String description,
            boolean transitive) {
        super();
        final String illegal = JkUtilsString.firstMatching(name, ",", "->");
        if (illegal != null) {
            throw new IllegalArgumentException("Scope name can't contain '" + illegal + "'");
        }
        this.extendedScopes = Collections.unmodifiableSet(extendedScopes);
        this.name = name;
        this.description = description;
        this.transitive = transitive;
    }

    public static JkScope of(String name, String description, boolean transitive, JkScope ... extending) {
        return new JkScope(name, JkUtilsIterable.setOf(extending), description, transitive);
    }

    public static JkScope ofMavenScope(String name) {
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
     * Scopes that are extended by this one.
     *
     */
    public Set<JkScope> getExtendedScopes() {
        return this.extendedScopes;
    }

    /**
     * Returns <code>true</code> if the dependencies defined with this scope should be resolved recursively
     * (meaning returning the dependencies of the dependencies and so on)
     */
    public boolean isTransitive() {
        return this.transitive;
    }

    /**
     * Returns scopes this scope inherits from. It returns recursively parent scopes, parent of parent scopes
     * and so on.
     */
    public List<JkScope> getAncestorScopes() {
        final List<JkScope> list = new LinkedList<>();
        list.add(this);
        for (final JkScope scope : this.extendedScopes) {
            for (final JkScope jkScope : scope.getAncestorScopes()) {
                if (!list.contains(jkScope)) {
                    list.add(jkScope);
                }
            }
        }
        return list;
    }

    /**
     * Returns this scope or its first ancestors found present in the specified scopes.
     */
    public List<JkScope> getCommonScopes(Collection<JkScope> scopes) {
        if (scopes.contains(this)) {
            return JkUtilsIterable.listOf(this);
        }
        final List<JkScope> result = new LinkedList<>();
        for (final JkScope scope : this.extendedScopes) {
            if (scopes.contains(scope)) {
                result.add(scope);
            } else {
                result.addAll(scope.getCommonScopes(scopes));
            }
        }
        return result;
    }

    /**
     * Returns <code>true</code> if this scope extends the specified one.
     */
    public boolean isExtending(JkScope jkScope) {
        if (extendedScopes == null || extendedScopes.isEmpty()) {
            return false;
        }
        for (final JkScope parent : extendedScopes) {
            if (parent.equals(jkScope) || parent.isExtending(jkScope)) {
                return true;
            }
        }
        return false;
    }



    /**
     * Returns a {@link JkScopeMapping} from this {@link JkScope} to the specified one.
     */
    public JkScopeMapping mapTo(String ... targetScopes) {
        return JkScopeMapping.of(this).to(targetScopes);
    }

    /**
     * Returns <code>true</code> if this scope is one or is extending any of the specified scopes.
     */
    public boolean isInOrIsExtendingAnyOf(Iterable<? extends JkScope> scopes) {
        for (final JkScope scope : scopes) {
            if (scope.equals(this) || this.isExtending(scope)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see #isInOrIsExtendingAnyOf(Iterable)
     */
    public boolean isInOrIsExtendingAnyOf(JkScope... scopes) {
        return isInOrIsExtendingAnyOf(Arrays.asList(scopes));
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

    /**
     * returns all specified scopes and all of their ancestors.
     */
    public static Set<JkScope> getInvolvedScopes(Iterable<JkScope> scopes) {
        final Set<JkScope> result = JkUtilsIterable.setOf(scopes);
        for (final JkScope jkScope : scopes) {
            result.addAll(jkScope.getAncestorScopes());
        }
        return result;
    }


}
