package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsIterable;

/**
 * A mapping to scopes to scopes acting when declaring dependencies. The goal of a scope mapping is to determine :<ul>
 * <li>which scopes a dependency is declared for</li>
 * <li>for each scope a dependency is declared, which scopes of its transitive dependencies to retrieve</li>
 * </ul>.
 *
 * For example, Your component 'A' depends of component 'B' for compiling. You can declare 'A' depends of 'B' with scope 'compile'. <br/>
 * Now imagine that for compiling, 'A' needs also the test class of 'B' along the dependencies 'B' needs for testing. For such, you
 * can declare a scope mapping as 'compile->compile, test'.
 *
 * This concept matches strictly with the <i>configuration</i> concept found in Ivy : <a href="http://wrongnotes.blogspot.be/2014/02/simplest-explanation-of-ivy.html">see here.</a>.
 */
public final class JkScopeMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A scope mapping active for any scope that map to the default scope of the dependencies.
     */
    public static final JkScopeMapping ALL_TO_DEFAULT = JkScope.of("*").mapTo(JkScope.of("default(*)"));

    // -------- Factory methods ----------------------------

    /**
     * Returns a partially constructed mapping specifying only scope entries and
     * willing for the mapping values.
     */
    public static JkScopeMapping.Partial of(JkScope... scopes) {
        return of(Arrays.asList(scopes));
    }

    /**
     * Returns a partially constructed mapping specifying only scope entries and
     * willing for the mapping values.
     */
    @SuppressWarnings("unchecked")
    public static JkScopeMapping.Partial of(Iterable<JkScope> scopes) {
        return new Partial(scopes, new JkScopeMapping(Collections.EMPTY_MAP));
    }

    /**
     * Creates an empty scope mapping.
     */
    @SuppressWarnings("unchecked")
    public static JkScopeMapping empty() {
        return new JkScopeMapping(Collections.EMPTY_MAP);
    }

    // ---------------- Instance members ---------------------------

    private final Map<JkScope, Set<JkScope>> map;

    private JkScopeMapping(Map<JkScope, Set<JkScope>> map) {
        super();
        this.map = map;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + map.hashCode();
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
        final JkScopeMapping other = (JkScopeMapping) obj;
        return (map.equals(other.map));
    }

    /**
     * Returns a partial object to construct a scope mapping identical to this one but augmented with the specified
     * mapping. The specified arguments stands for the left side scopes of the mapping to be construct.
     */
    public Partial and(JkScope... from) {
        return and(Arrays.asList(from));
    }

    /**
     * @see #and(JkScope...)
     */
    public Partial and(Iterable<JkScope> from) {
        return new Partial(from, this);
    }

    private JkScopeMapping andFromTo(JkScope from, Iterable<JkScope> to) {
        final Map<JkScope, Set<JkScope>> result = new HashMap<JkScope, Set<JkScope>>(map);
        if (result.containsKey(from)) {
            final Set<JkScope> list = result.get(from);
            final Set<JkScope> newList = new HashSet<JkScope>(list);
            newList.addAll(JkUtilsIterable.listOf(to));
            result.put(from, Collections.unmodifiableSet(newList));
        } else {
            final Set<JkScope> newList = new HashSet<JkScope>();
            newList.addAll(JkUtilsIterable.listOf(to));
            result.put(from, Collections.unmodifiableSet(newList));
        }
        return new JkScopeMapping(result);
    }

    /**
     * Returns the right side scope mapped to the specified left scope.
     */
    public Set<JkScope> mappedScopes(JkScope sourceScope) {
        final Set<JkScope> result = this.map.get(sourceScope);
        if (result != null && !result.isEmpty()) {
            return result;
        }
        throw new IllegalArgumentException("No mapped scope declared for " + sourceScope
                + ". Declared scopes are " + this.entries());
    }

    /**
     * Returns all the scopes declared on the left side of this scope mapping.
     */
    public Set<JkScope> entries() {
        return Collections.unmodifiableSet(this.map.keySet());
    }

    /**
     * Returns all scopes : the ones declared on left side plus yhe ones declared
     * on right side.
     */
    public Set<JkScope> declaredScopes() {
        final Set<JkScope> result = new HashSet<JkScope>();
        result.addAll(entries());
        for (final JkScope scope : entries()) {
            result.addAll(this.map.get(scope));
        }
        return result;
    }

    @Override
    public String toString() {
        return map.toString();
    }

    /**
     * Partial object to construct a scope mapping. The partial object contains only the left side
     * of a scope mapping entries.
     */
    public static class Partial {

        private final Iterable<JkScope> from;

        private final JkScopeMapping mapping;

        private Partial(Iterable<JkScope> from, JkScopeMapping mapping) {
            super();
            this.from = from;
            this.mapping = mapping;
        }

        /**
         * Returns a scope mapping by by specifying the scope mapped to to this left one.
         */
        public JkScopeMapping to(JkScope... targets) {
            return to(Arrays.asList(targets));
        }

        /**
         * Similar to {@link #to(JkScope...)} but allow raw string as parameter
         */
        public JkScopeMapping to(String... targets) {
            final List<JkScope> list = new LinkedList<JkScope>();
            for (final String target : targets) {
                list.add(JkScope.of(target));
            }
            return to(list);
        }

        /**
         * @see #to(JkScope...)
         */
        public JkScopeMapping to(Iterable<JkScope> targets) {
            JkScopeMapping result = mapping;
            for (final JkScope fromScope : from) {
                for (final JkScope toScope : targets) {
                    result = result.andFromTo(fromScope, JkUtilsIterable.setOf(toScope));
                }
            }
            return result;
        }

    }

}