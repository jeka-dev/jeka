package org.jerkar.depmanagement;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.utils.JkUtilsIterable;

public final class JkScopeMapping implements Serializable {

	// -------- Factory methods ----------------------------

	private static final long serialVersionUID = 1L;

	/**
	 * Returns a partially constructed mapping specifying only scope entries and
	 * willing for the mapping values.
	 */
	public static JkScopeMapping.Partial of(JkScope ...scopes) {
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

	public Partial and(JkScope ...from) {
		return and(Arrays.asList(from));
	}

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

	public Set<JkScope> mappedScopes(JkScope sourceScope) {
		final Set<JkScope> result = this.map.get(sourceScope);
		if (result != null && !result.isEmpty()) {
			return result;
		}
		throw new IllegalArgumentException("No mapped scope declared for " + sourceScope + ". Declared scopes are " + this.entries());
	}

	public Set<JkScope> entries() {
		return Collections.unmodifiableSet(this.map.keySet());
	}

	public Set<JkScope> involvedScopes() {
		final Set<JkScope> result = new HashSet<JkScope>();
		result.addAll(entries());
		for (final JkScope scope : entries()) {
			result.addAll(this.map.get(scope));
		}
		return Collections.unmodifiableSet(result);
	}

	@Override
	public String toString() {
		return map.toString();
	}



	public static class Partial {

		private final Iterable<JkScope> from;

		private final JkScopeMapping mapping;

		private Partial(Iterable<JkScope> from, JkScopeMapping mapping) {
			super();
			this.from = from;
			this.mapping = mapping;
		}

		public JkScopeMapping to(JkScope... targets) {
			return to(Arrays.asList(targets));
		}

		public JkScopeMapping to(String... targets) {
			final List<JkScope> list = new LinkedList<JkScope>();
			for (final String target : targets) {
				list.add(JkScope.of(target));
			}
			return to(list);
		}

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