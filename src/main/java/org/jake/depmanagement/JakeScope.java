package org.jake.depmanagement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jake.utils.JakeUtilsIterable;

/**
 * Defines a context where is defined dependencies of a given project.
 * According we need to compile, test or run the application, the dependencies may diverge.
 * For example, <code>Junit</code> library may only be necessary for testing, so we
 * can declare that <code>Junit</scope> is only necessary for scope <code>TEST</code>.<br/>
 * This class predefines some standard scope as <code>COMPILE</code> or <code>RUNTIME</code>
 * but you may define you own.
 * 
 * Similar to Maven <code>scope</code> or Ivy <code>configuration</code>.
 * 
 * @author Djeang
 */
public final class JakeScope {

	/**
	 * Dependencies to compile the project but that should not be embedded in produced artifacts.
	 */
	public static JakeScope PROVIDED = JakeScope.of("provided");

	/**
	 * Dependencies to compile the project.
	 */
	public static JakeScope COMPILE = JakeScope.of("compile", PROVIDED);

	/**
	 * Dependencies to embed in produced artifacts (as war or fat jar * files).
	 */
	public static JakeScope RUNTIME = JakeScope.of("runtime", COMPILE.excluding(PROVIDED));

	/**
	 * Dependencies necessary to compile and run tests.
	 */
	public static JakeScope TEST = JakeScope.of("test", RUNTIME, PROVIDED);

	@SuppressWarnings("unchecked")
	public static JakeScope of(String name, JakeScope ...inheritFroms) {
		return new JakeScope(name, Arrays.asList(inheritFroms), Collections.EMPTY_LIST);
	}

	private final List<JakeScope> inheritFrom;

	private final List<JakeScope> excluding;

	private final String name;

	private JakeScope(String name, List<JakeScope> inheritFrom, List<JakeScope> excluding) {
		super();
		this.inheritFrom = inheritFrom;
		this.name = name;
		this.excluding = excluding;
	}

	public JakeScope excluding(JakeScope... jakeScopes) {
		final List<JakeScope> exclues = new LinkedList<JakeScope>(this.excluding);
		exclues.addAll(Arrays.asList(jakeScopes));
		return new JakeScope(name, inheritFrom, exclues);
	}

	public String name() {
		return name;
	}

	public List<JakeScope> impliedScopes() {
		final List<JakeScope> list = new LinkedList<JakeScope>();
		list.add(this);
		for (final JakeScope scope : this.inheritFrom) {
			if (!excluding.contains(scope)) {
				for (final JakeScope jakeScope : scope.impliedScopes()) {
					if (!list.contains(jakeScope)) {
						list.add(jakeScope);
					}
				}

			}
		}
		return list;
	}

	public Mappings mapTo(JakeScope targetScope) {
		return Mappings.of(this, targetScope);
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
		final JakeScope other = (JakeScope) obj;
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
		return "Scope:"+name;
	}

	private static class Mapping implements Iterable<Mapping> {

		private final JakeScope from;
		private final JakeScope to;

		public Mapping(JakeScope from, JakeScope to) {
			super();
			this.from = from;
			this.to = to;
		}

		@Override
		public Iterator<Mapping> iterator() {
			return JakeUtilsIterable.of(this).iterator();
		}

	}

	public static class Mappings implements Iterable<Mapping> {

		private final List<Mapping> list;

		private Mappings(List<Mapping> list) {
			super();
			this.list = list;
		}

		public static Mappings of(JakeScope from, JakeScope to) {
			final List<Mapping> scopes = new ArrayList<Mapping>();
			scopes.add(new Mapping(from, to));
			return new Mappings(scopes);
		}

		public static Mappings of(JakeScope from, String to) {
			return of(from, JakeScope.of(to));
		}

		public Mappings and(JakeScope from, JakeScope to) {
			final List<Mapping> scopes = new ArrayList<Mapping>(this.list);
			scopes.add(new Mapping(from, to));
			return new Mappings(scopes);
		}

		public Mappings and(JakeScope from, String to) {
			return and(from, JakeScope.of(to));
		}

		public Mappings and(JakeScope from) {
			return and(from, from);
		}

		@Override
		public Iterator<Mapping> iterator() {
			return list.iterator();
		}

		public Set<JakeScope> getTargetScope(JakeScope sourceScope) {
			final HashSet<JakeScope> set = new HashSet<JakeScope>();
			for (final Mapping mapping : list) {
				if (sourceScope.equals(mapping.from)) {
					set.add(mapping.to);
				}
			}
			return set;
		}


	}

}
