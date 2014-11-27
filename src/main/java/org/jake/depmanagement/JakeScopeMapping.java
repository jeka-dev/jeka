package org.jake.depmanagement;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jake.utils.JakeUtilsIterable;

public final class JakeScopeMapping {

	// -------- Factory methods ----------------------------

	/**
	 * Returns a partially constructed mapping specifying only scope entries and
	 * willing for the mapping values.
	 */
	public static JakeScopeMapping.Partial of(JakeScope ...scopes) {
		return of(Arrays.asList(scopes));
	}

	/**
	 * Returns a partially constructed mapping specifying only scope entries and
	 * willing for the mapping values.
	 */
	@SuppressWarnings("unchecked")
	public static JakeScopeMapping.Partial of(Iterable<JakeScope> scopes) {
		return new Partial(scopes, new JakeScopeMapping(Collections.EMPTY_MAP));
	}


	// ---------------- Instance members ---------------------------


	private final Map<JakeScope, Set<JakeScope>> map;

	private JakeScopeMapping(Map<JakeScope, Set<JakeScope>> map) {
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
		final JakeScopeMapping other = (JakeScopeMapping) obj;
		return (!map.equals(other.map));
	}

	public Partial and(JakeScope ...from) {
		return and(Arrays.asList(from));
	}

	public Partial and(Iterable<JakeScope> from) {
		return new Partial(from, this);
	}

	private JakeScopeMapping andFromTo(JakeScope from, Iterable<JakeScope> to) {
		final Map<JakeScope, Set<JakeScope>> result = new HashMap<JakeScope, Set<JakeScope>>(map);
		if (result.containsKey(from)) {
			final Set<JakeScope> list = result.get(from);
			final Set<JakeScope> newList = new HashSet<JakeScope>(list);
			newList.addAll(JakeUtilsIterable.toList(to));
			result.put(from, Collections.unmodifiableSet(newList));
		} else {
			final Set<JakeScope> newList = new HashSet<JakeScope>();
			newList.addAll(JakeUtilsIterable.toList(to));
			result.put(from, Collections.unmodifiableSet(newList));
		}
		return new JakeScopeMapping(result);
	}

	public Set<JakeScope> mappedScopes(JakeScope sourceScope) {
		final Set<JakeScope> result = this.map.get(sourceScope);
		if (result != null && !result.isEmpty()) {
			return result;
		}
		throw new IllegalArgumentException("No mapped scope declared for " + sourceScope + ". Declared scopes are " + this.entries());
	}

	public Set<JakeScope> entries() {
		return Collections.unmodifiableSet(this.map.keySet());
	}

	public Set<JakeScope> involvedScopes() {
		final Set<JakeScope> result = new HashSet<JakeScope>();
		result.addAll(entries());
		for (final JakeScope scope : entries()) {
			result.addAll(this.map.get(scope));
		}
		return Collections.unmodifiableSet(result);
	}

	@Override
	public String toString() {
		return map.toString();
	}



	public static class Partial {

		private final Iterable<JakeScope> from;

		private final JakeScopeMapping mapping;

		private Partial(Iterable<JakeScope> from, JakeScopeMapping mapping) {
			super();
			this.from = from;
			this.mapping = mapping;
		}

		public JakeScopeMapping to(JakeScope... targets) {
			return to(Arrays.asList(targets));
		}

		public JakeScopeMapping to(String... targets) {
			final List<JakeScope> list = new LinkedList<JakeScope>();
			for (final String target : targets) {
				list.add(JakeScope.of(target));
			}
			return to(list);
		}

		public JakeScopeMapping to(Iterable<JakeScope> targets) {
			JakeScopeMapping result = mapping;
			for (final JakeScope fromScope : from) {
				for (final JakeScope toScope : targets) {
					result = result.andFromTo(fromScope, JakeUtilsIterable.setOf(toScope));
				}
			}
			return result;
		}

	}

}