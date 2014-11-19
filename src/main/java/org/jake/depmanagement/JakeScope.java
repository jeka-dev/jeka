package org.jake.depmanagement;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
	 * Stands for the artifacts produced by the build without any dependencies.
	 * This scope is primarily intended for scoping publication, not for dependencies dependenies.
	 */
	public static final JakeScope ARCHIVES = JakeScope.of("archives");

	/**
	 * Creates a new {@link JakeScope} passing its name and inherited scopes.
	 * @param name The name of the scope : should be unique within a build.
	 * @param inheritFroms Inherited scopes.
	 */
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

	/**
	 * Returns a <code>JakeScope</code> equals to this one but excluding the specified <code>JakeScope<code>s.
	 * @param jakeScopes
	 * @return
	 */
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

	public boolean inheritFrom(JakeScope jakeScope) {
		if (inheritFrom == null || inheritFrom.isEmpty()) {
			return false;
		}
		for (final JakeScope parent : inheritFrom) {
			if (parent.equals(jakeScope) || parent.inheritFrom(jakeScope)) {
				return true;
			}
		}
		return false;
	}

	public JakeScopeMapping mapTo(JakeScope targetScope) {
		return JakeScopeMapping.of(this, targetScope);
	}

	public boolean isInOrIsInheritedByAnyOf(Iterable<JakeScope> scopes) {
		for (final JakeScope scope : scopes) {
			if (scope.equals(this) || scope.inheritFrom(this)) {
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

}
