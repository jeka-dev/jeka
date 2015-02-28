package org.jake.depmanagement;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jake.utils.JakeUtilsString;

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
		return new OptionableScope(name, Collections.EMPTY_SET, "", true, true);
	}

	private final Set<JakeScope> extendedScopes;

	private final String name;

	private final String description;

	private final boolean transitive;

	private final boolean isPublic;

	private JakeScope(String name, Set<JakeScope> extendedScopes, String description, boolean transitive, boolean isPublic) {
		super();
		final String illegal = JakeUtilsString.containsAnyOf(name, ",", "->");
		if (illegal != null) {
			throw new IllegalArgumentException("Scope name can't contain '" + illegal + "'");
		}
		this.extendedScopes = Collections.unmodifiableSet(extendedScopes);
		this.name = name;
		this.description = description;
		this.transitive = transitive;
		this.isPublic = isPublic;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public Set<JakeScope> extendedScopes() {
		return this.extendedScopes;
	}

	public boolean transitive() {
		return this.transitive;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public List<JakeScope> ancestorScopes() {
		final List<JakeScope> list = new LinkedList<JakeScope>();
		list.add(this);
		for (final JakeScope scope : this.extendedScopes) {
			for (final JakeScope jakeScope : scope.ancestorScopes()) {
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
		return JakeScopeMapping.of(this).to(targetScope);
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
		return name;
	}

	/**
	 * A {@link JakeScope} allowing to define other scope from it. It exists only to serve the fluent
	 * API purpose as for clarity we can't create derived <code>scopes</scope> directly from a {@link JakeScope} .<br/>
	 * Use the {@link #descr(String)} method last as it returns a {@link JakeScope}.
	 * 
	 * @author Jerome Angibaud
	 */
	public static class OptionableScope extends JakeScope {

		private OptionableScope(String name, Set<JakeScope> extendedScopes, String descr, boolean transitive, boolean isPublic) {
			super(name, extendedScopes, descr, transitive, isPublic);
		}

		public OptionableScope extending(JakeScope ...scopes) {
			return new OptionableScope(name(), new HashSet<JakeScope>(Arrays.asList(scopes)), description(), transitive(), isPublic());
		}

		public OptionableScope transitive(boolean transitive) {
			return new OptionableScope(name(), extendedScopes(), description(), transitive, isPublic());
		}

		public OptionableScope isPublic(boolean isPublic) {
			return new OptionableScope(name(), extendedScopes(), description(), transitive(), isPublic);
		}

		public JakeScope descr(String description) {
			return new JakeScope(name(), extendedScopes(), description, transitive(), isPublic());
		}



	}

}
