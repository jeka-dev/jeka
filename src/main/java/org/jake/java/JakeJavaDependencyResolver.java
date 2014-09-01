package org.jake.java;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Defines where are located required dependencies for various scope.
 * 
 * @author Djeang
 */
public abstract class JakeJavaDependencyResolver {

	/**
	 * All libraries finally used to compile the production code.
	 */
	public abstract JakeClasspath compile();

	/**
	 * All libraries finally used both for compile and run test.
	 */
	public abstract JakeClasspath test();

	/**
	 * All libraries finally to be embedded in deliveries (as war or fat jar
	 * files). It contains generally dependencies needed for compilation plus
	 * extra runtime-only dependencies. So that's the general rule that libs
	 * contained in {@link #compile()} are contained in {@link #runtime()} as
	 * well.
	 */
	public abstract JakeClasspath runtime();

	public JakeJavaDependencyResolver merge(JakeJavaDependencyResolver other,
			File otherClasses, File otherTestClasses) {
		return new MergedDependencyResolver(this, other, otherClasses,
				otherTestClasses);
	}

	public List<String> toStrings() {
		final List<String> result = new LinkedList<String>();
		result.add(this.getClass().getName());
		final JakeClasspath compileLibs = compile();
		final JakeClasspath runtimeLibs = runtime();
		final JakeClasspath testLibs = test();
		result.add("compile (" + compileLibs.files().size() + " libs): "
				+ compileLibs);
		result.add("runtime (" + runtimeLibs.files().size() + " libs): "
				+ runtimeLibs);
		result.add("test (" + testLibs.files().size() + " libs): " + testLibs);
		return result;
	}

	public boolean isEmpty() {
		return compile().isEmpty() && test().isEmpty() && runtime().isEmpty();
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
		public JakeClasspath compile() {
			return base.compile().with(otherClasses).with(other.compile());
		}

		@Override
		public JakeClasspath test() {
			return base.test().with(otherTestClasses).with(other.test());
		}

		@Override
		public JakeClasspath runtime() {
			return base.runtime().with(other.runtime());

		}

	}

}