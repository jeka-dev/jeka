package org.jake.java;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jake.JakeLog;
import org.jake.utils.JakeUtilsReflect;

/**
 * Defines where are located required dependencies for various scope.
 * 
 * @author Djeang
 */
public abstract class JakeJavaDependencyResolver {

	/**
	 * All libraries finally used to compile the production code.
	 */
	public abstract JakeClasspath compileScope();

	/**
	 * All libraries finally used both for compile and run test.
	 */
	public abstract JakeClasspath testScope();

	/**
	 * All libraries finally to be embedded in deliveries (as war or fat jar
	 * files). It contains generally dependencies needed for compilation plus
	 * extra runtime-only dependencies. So that's the general rule that libs
	 * contained in {@link #compileScope()} are contained in {@link #runtimeScope()} as
	 * well.
	 */
	public abstract JakeClasspath runtimeScope();

	public JakeJavaDependencyResolver merge(JakeJavaDependencyResolver other,
			File otherClasses, File otherTestClasses) {
		return new MergedDependencyResolver(this, other, otherClasses,
				otherTestClasses);
	}

	public List<String> toStrings() {
		final List<String> result = new LinkedList<String>();
		result.add(this.getClass().getName());
		final JakeClasspath compileLibs = compileScope();
		final JakeClasspath runtimeLibs = runtimeScope();
		final JakeClasspath testLibs = testScope();
		result.add("compile (" + compileLibs.entries().size() + " libs): "
				+ compileLibs);
		result.add("runtime (" + runtimeLibs.entries().size() + " libs): "
				+ runtimeLibs);
		result.add("test (" + testLibs.entries().size() + " libs): " + testLibs);
		return result;
	}

	public boolean isEmpty() {
		return compileScope().isEmpty() && testScope().isEmpty() && runtimeScope().isEmpty();
	}

	public static JakeJavaDependencyResolver findByClassName(String simpleOrFullClassName) {
		final Class<? extends JakeJavaDependencyResolver> depClass =
				JakeClassLoader.current().loadFromNameOrSimpleName(simpleOrFullClassName, JakeJavaDependencyResolver.class);
		if (depClass == null) {
			JakeLog.warn("Class " + simpleOrFullClassName
					+ " not found or it is not a "
					+ JakeJavaDependencyResolver.class.getName() + ".");
			return null;
		}
		return JakeUtilsReflect.newInstance(depClass);
	}

	public static JakeJavaDependencyResolver findByClassNameOrDfault(String simpleOrFullClassName, JakeJavaDependencyResolver defaultResolver) {
		if (simpleOrFullClassName == null) {
			return defaultResolver;
		}
		final JakeJavaDependencyResolver result = findByClassName(simpleOrFullClassName);
		if(result == null) {
			return defaultResolver;
		}
		return defaultResolver;
	}

	private class MergedDependencyResolver extends JakeJavaDependencyResolver {

		private final JakeJavaDependencyResolver base;

		private final JakeJavaDependencyResolver other;

		private final File otherClasses;

		private final File otherTestClasses;

		public MergedDependencyResolver(JakeJavaDependencyResolver base,
				JakeJavaDependencyResolver other, File otherClasses,
				File otherTestClasses) {
			super();
			this.base = base;
			this.other = other;
			this.otherClasses = otherClasses;
			this.otherTestClasses = otherTestClasses;
		}

		@Override
		public JakeClasspath compileScope() {
			return base.compileScope().and(otherClasses).and(other.compileScope());
		}

		@Override
		public JakeClasspath testScope() {
			return base.testScope().and(otherTestClasses).and(other.testScope());
		}

		@Override
		public JakeClasspath runtimeScope() {
			return base.runtimeScope().and(other.runtimeScope());

		}

	}

}