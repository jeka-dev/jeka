package org.jake.java;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jake.utils.JakeUtilsIterable;

/**
 * Defines where are located required dependencies for various scope.
 * 
 * @author Djeang
 */
public abstract class JakeJavaDependencyResolver {

	/**
	 * All libraries finally used to compile the production code.
	 */
	public abstract List<File> compile();

	/**
	 * All libraries finally used both for compile and run test.
	 */
	public abstract List<File> test();

	/**
	 * All libraries finally to be embedded in deliveries (as war or fat jar files). It contains
	 * generally dependencies needed for compilation plus extra runtime-only dependencies. So that's the
	 * general rule that libs contained in {@link #compile()} are contained in {@link #runtime()} as well.
	 */
	public abstract List<File> runtime();


	public JakeJavaDependencyResolver merge(JakeJavaDependencyResolver other, File otherClasses, File otherTestClasses) {
		return new MergedDependencyResolver(this, other, otherClasses, otherTestClasses);
	}


	public List<String> toStrings() {
		final List<String> result = new LinkedList<String>();
		result.add(this.getClass().getSimpleName());
		final List<File> compileLibs = compile();
		final List<File> runtimeLibs = runtime();
		final List<File> testLibs = test();
		result.add("compile (" + compileLibs.size()+ " libs): " + JakeUtilsIterable.toString(compile(),  ";"));
		result.add("runtime (" + runtimeLibs.size()+ " libs): " + JakeUtilsIterable.toString(runtime(), ";"));
		result.add("test ("+testLibs.size()+ " libs): " + JakeUtilsIterable.toString(test(), ";"));
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
				JakeJavaDependencyResolver other, File otherClasses, File otherTestClasses) {
			super();
			this.base = base;
			this.other = other;
			this.otherClasses = otherClasses;
			this.otherTestClasses = otherTestClasses;
		}


		@SuppressWarnings("unchecked")
		@Override
		public List<File> compile() {
			return JakeUtilsIterable.concatLists(base.compile(), JakeUtilsIterable.single(otherClasses), other.compile() );
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<File> test() {
			return JakeUtilsIterable.concatLists(base.test(), JakeUtilsIterable.single(otherTestClasses), other.test() );
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<File> runtime() {
			return JakeUtilsIterable.concatLists(base.runtime(), other.runtime() );

		}


	}

}