package org.jake.java;

import java.io.File;
import java.util.List;

import org.jake.utils.IterableUtils;

/**
 * Defines where are located required dependencies for various scope.
 * 
 * @author Djeang
 */
public abstract class DependencyResolver {

	/**
	 * All libraries finally used to compile the production code.
	 */
	public abstract List<File> compileDependencies();

	/**
	 * All libraries finally used both for compile and run test. 
	 */
	public abstract List<File> testDependencies();

	/**
	 * All libraries finally to be embedded in deliveries (as war or fat jar files). It contains 
	 * generally dependencies needed for compilation plus extra runtime-only dependencies.
	 */
	public abstract List<File> runtimeDependencies();
	
		
	public DependencyResolver merge(DependencyResolver other, File otherClasses, File otherTestClasses) {
		return new TransitiveDependencyResolver(this, other, otherClasses, otherTestClasses);
	} 
	
	protected class TransitiveDependencyResolver extends DependencyResolver {
		
		private final DependencyResolver base;
		
		private final DependencyResolver other;
		
		private final File otherClasses;
		
		private final File otherTestClasses;

		
		public TransitiveDependencyResolver(DependencyResolver base,
				DependencyResolver other, File otherClasses, File otherTestClasses) {
			super();
			this.base = base;
			this.other = other;
			this.otherClasses = otherClasses;
			this.otherTestClasses = otherTestClasses;
		}
		
	
		public TransitiveDependencyResolver(DependencyResolver base,
				DependencyResolver other, File otherClasses) {
			super();
			this.base = base;
			this.other = other;
			this.otherClasses = otherClasses;
			this.otherTestClasses = null;
		}
		
		
		public TransitiveDependencyResolver(DependencyResolver base,
				DependencyResolver other) {
			super();
			this.base = base;
			this.other = other;
			this.otherClasses = null;
			this.otherTestClasses = null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<File> compileDependencies() {
			return IterableUtils.concatLists(base.compileDependencies(), IterableUtils.single(otherClasses), other.compileDependencies() );
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<File> testDependencies() {
			return IterableUtils.concatLists(base.testDependencies(), IterableUtils.single(otherTestClasses), other.testDependencies() );
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<File> runtimeDependencies() {
			return IterableUtils.concatLists(base.runtimeDependencies(), other.runtimeDependencies() );

		}
		
		
	}

}