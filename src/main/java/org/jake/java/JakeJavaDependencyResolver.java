package org.jake.java;

import java.io.File;
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
	 * generally dependencies needed for compilation plus extra runtime-only dependencies.
	 */
	public abstract List<File> runtime();


	public JakeJavaDependencyResolver merge(JakeJavaDependencyResolver other, File otherClasses, File otherTestClasses) {
		return new TransitiveDependencyResolver(this, other, otherClasses, otherTestClasses);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getSimpleName()).append("\n");
		builder.append("compile: ").append(JakeUtilsIterable.toString(compile(), ";")).append("\n");
		builder.append("runtime: ").append(JakeUtilsIterable.toString(runtime(), ";")).append("\n");
		builder.append("test: ").append(JakeUtilsIterable.toString(test(), ";"));
		return builder.toString();
	}


	public boolean isEmpty() {
		return compile().isEmpty() && test().isEmpty() && runtime().isEmpty();
	}

	protected class TransitiveDependencyResolver extends JakeJavaDependencyResolver {

		private final JakeJavaDependencyResolver base;

		private final JakeJavaDependencyResolver other;

		private final File otherClasses;

		private final File otherTestClasses;


		public TransitiveDependencyResolver(JakeJavaDependencyResolver base,
				JakeJavaDependencyResolver other, File otherClasses, File otherTestClasses) {
			super();
			this.base = base;
			this.other = other;
			this.otherClasses = otherClasses;
			this.otherTestClasses = otherTestClasses;
		}


		public TransitiveDependencyResolver(JakeJavaDependencyResolver base,
				JakeJavaDependencyResolver other, File otherClasses) {
			super();
			this.base = base;
			this.other = other;
			this.otherClasses = otherClasses;
			this.otherTestClasses = null;
		}


		public TransitiveDependencyResolver(JakeJavaDependencyResolver base,
				JakeJavaDependencyResolver other) {
			super();
			this.base = base;
			this.other = other;
			this.otherClasses = null;
			this.otherTestClasses = null;
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