package org.jake.depmanagement;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jake.JakeClassLoader;
import org.jake.JakeLog;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsReflect;

/**
 * Defines where are located required dependencies for various scopes.
 * 
 * @author Djeang
 */
public abstract class JakeDependencyResolver {

	protected abstract List<File> getDeclaredDependencies(JakeScope scope);

	public abstract Set<JakeScope> declaredScopes();

	public final List<File> get(JakeScope scope) {
		final List<File> result = new LinkedList<File>();
		for (final JakeScope jakeScope : scope.impliedScopes()) {
			result.addAll(this.getDeclaredDependencies(jakeScope));
		}
		return result;
	}


	public JakeDependencyResolver merge(JakeDependencyResolver other) {
		return new MergedDependencyResolver(this, other);
	}

	public List<String> toStrings() {
		final List<String> result = new LinkedList<String>();
		result.add(this.getClass().getName());
		for (final JakeScope scope : this.declaredScopes()) {
			final List<File> libs = this.get(scope);
			result.add(scope.name() + " (" + libs.size() + " artifacts): " + libs);
		}
		return result;
	}

	public boolean isEmpty() {
		for (final JakeScope scope : this.declaredScopes()) {
			if (!this.get(scope).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private static JakeDependencyResolver findByClassName(String simpleOrFullClassName) {
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

	public static JakeDependencyResolver findByClassNameOrDfault(String simpleOrFullClassName, JakeDependencyResolver defaultResolver) {
		if (simpleOrFullClassName == null) {
			return defaultResolver;
		}
		final JakeDependencyResolver result = findByClassName(simpleOrFullClassName);
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