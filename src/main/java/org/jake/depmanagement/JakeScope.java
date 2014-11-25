package org.jake.depmanagement;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Defines a context where is defined dependencies of a given project.
 * According we need to compile, test or run the application, the dependencies may diverge.
 * For example, <code>Junit</code> library may only be necessary for testing, so we
 * can declare that <code>Junit</scope> is only necessary for scope <code>TEST</code>.<br/>
 * 
 * Similar to Maven <code>scope</code> or Ivy <code>configuration</code>.
 * 
 * @author Jerome Angibaud
 */
public class JakeScope {

	/**
	 * Creates a new {@link JakeScope} passing its name.
	 */
	@SuppressWarnings("unchecked")
	public static OptionableScope of(String name) {
		return new OptionableScope(name, Collections.EMPTY_SET);
	}

	private final Set<JakeScope> extendedScopes;

	private final String name;

	private JakeScope(String name, Set<JakeScope> extendedScopes) {
		super();
		this.extendedScopes = Collections.unmodifiableSet(extendedScopes);
		this.name = name;
	}

	public String name() {
		return name;
	}

	public Set<JakeScope> extendedScopes() {
		return this.extendedScopes;
	}

	public List<JakeScope> impliedScopes() {
		final List<JakeScope> list = new LinkedList<JakeScope>();
		list.add(this);
		for (final JakeScope scope : this.extendedScopes) {
			for (final JakeScope jakeScope : scope.impliedScopes()) {
				if (!list.contains(jakeScope)) {
					list.add(jakeScope);
				}
			}
		}
		return list;
	}

	public boolean isExtending(JakeScope jakeScope) {
		if (extendedScopes == null || extendedScopes.isEmpty()) {
			return false;
		}
		for (final JakeScope parent : extendedScopes) {
			if (parent.equals(jakeScope) || parent.isExtending(jakeScope)) {
				return true;
			}
		}
		return false;
	}

	public JakeScopeMapping mapTo(JakeScope targetScope) {
		return JakeScopeMapping.of(this, targetScope);
	}

	public boolean isInOrIsExtendingAnyOf(Iterable<? extends JakeScope> scopes) {
		for (final JakeScope scope : scopes) {
			if (scope.equals(this) || scope.isExtending(this)) {
				return true;
			}
		}
		return false;
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

	public static class OptionableScope extends JakeScope {

		private OptionableScope(String name, Set<JakeScope> extendedScopes) {
			super(name, extendedScopes);
		}

		public OptionableScope extending(JakeScope ...scopes) {
			return new OptionableScope(name(), new HashSet<JakeScope>(Arrays.asList(scopes)));
		}



	}

}
