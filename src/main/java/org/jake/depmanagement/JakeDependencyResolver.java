package org.jake.depmanagement;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jake.JakeClassLoader;
import org.jake.JakeLog;
import org.jake.JakePath;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsReflect;

/**
 * Defines where are located required dependencies for various scopes.
 * Each project has its own instance of <code>JakeDependencyResolver</code>.
 * 
 * @author Djeang
 */
public abstract class JakeDependencyResolver {

	protected abstract List<File> getDeclaredDependencies(JakeScope scope);

	/**
	 * Returns the scopes used inside this resolver.
	 */
	public abstract Set<JakeScope> declaredScopes();


	public final JakePath get(JakeScope scope) {
		final List<File> result = new LinkedList<File>();
		for (final JakeScope jakeScope : scope.impliedScopes()) {
			result.addAll(this.getDeclaredDependencies(jakeScope));
		}
		return JakePath.of(result);
	}

	/**
	 * Returns a resolver that is a merge of this one and the one passed as parameters.
	 * In other words, the returned resolver will contains dependencies of this resolver
	 * and the specified one.
	 */
	public JakeDependencyResolver merge(JakeDependencyResolver other) {
		return new MergedDependencyResolver(this, other);
	}

	/**
	 * Returns a multi-line human readable representation of this resolver.
	 */
	public List<String> toStrings() {
		final List<String> result = new LinkedList<String>();
		result.add(this.getClass().getName());
		for (final JakeScope scope : this.declaredScopes()) {
			final JakePath libs = this.get(scope);
			result.add(scope.name() + " (" + libs.entries().size() + " artifacts): " + libs);
		}
		return result;
	}

	/**
	 * Returns <code>true<code> if this resolver does not contain any dependencies.
	 */
	public boolean isEmpty() {
		for (final JakeScope scope : this.declaredScopes()) {
			if (!this.get(scope).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private static JakeDependencyResolver createByClassName(String simpleOrFullClassName) {
		final Class<? extends JakeDependencyResolver> depClass =
				JakeClassLoader.current().loadFromNameOrSimpleName(simpleOrFullClassName, JakeDependencyResolver.class);
		if (depClass == null) {
			JakeLog.warn("Class " + simpleOrFullClassName
					+ " not found or it is not a "
					+ JakeDependencyResolver.class.getName() + ".");
			return null;
		}
		return JakeUtilsReflect.newInstance(depClass);
	}

	/**
	 * Returns a dependency resolver according to its class name. The class name can be
	 * either a full qualified name (as org.mypackage.MyResolver) or simple class name (as MyResolver).
	 */
	public static JakeDependencyResolver createByClassNameOrUseDefault(String simpleOrFullClassName, JakeDependencyResolver defaultResolver) {
		if (simpleOrFullClassName == null) {
			return defaultResolver;
		}
		final JakeDependencyResolver result = createByClassName(simpleOrFullClassName);
		if(result == null) {
			return defaultResolver;
		}
		return defaultResolver;
	}

	private class MergedDependencyResolver extends JakeDependencyResolver {

		private final JakeDependencyResolver base;

		private final JakeDependencyResolver other;

		public MergedDependencyResolver(JakeDependencyResolver base,
				JakeDependencyResolver other) {
			super();
			this.base = base;
			this.other = other;
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<File> getDeclaredDependencies(JakeScope scope) {
			return JakeUtilsIterable.concatLists(base.get(scope), other.get(scope));
		}

		@Override
		public Set<JakeScope> declaredScopes() {
			final Set<JakeScope> result = new HashSet<JakeScope>();
			result.addAll(base.declaredScopes());
			result.addAll(other.declaredScopes());
			return result;
		}


	}

}