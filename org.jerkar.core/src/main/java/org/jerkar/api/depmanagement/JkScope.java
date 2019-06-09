package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dev.jeka.core.api.utils.JkUtilsIterable;
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
public final class JkScope implements Serializable {

    private static final long serialVersionUID = 1L;

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
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
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
