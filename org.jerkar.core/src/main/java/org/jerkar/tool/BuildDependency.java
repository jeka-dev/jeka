package org.jerkar.tool;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jerkar.api.depmanagement.JkProjectDependency;

/**
 * A dependency on files and Jerkar build definition of external project.
 * This class is particularly used in multi-project though this is rarely directly used by the
 * build definition authors.
 * 
 * @author Jerome Angibaud
 */
final class BuildDependency extends JkProjectDependency {

	private static final long serialVersionUID = 1L;

	private final JkBuild projectBuild;


	private BuildDependency(JkBuild projectBuild, Set<File> files) {
		super(runnable(projectBuild), files);
		this.projectBuild = projectBuild;
	}

	private static Runnable runnable(final JkBuild build) {
		return new Runnable() {

			@Override
			public void run() {
				build.doDefault();

			}
		};
	}

	public static BuildDependency of(JkBuild projectBuild, Set<File> files) {
		return new BuildDependency(projectBuild, new HashSet<File>(files));
	}

	public static BuildDependency of(JkBuild projectBuild, File... files) {
		return of(projectBuild, new HashSet<File>(Arrays.asList(files)));
	}

	public JkBuild projectBuild() {
		return projectBuild;
	}


	@Override
	public String toString() {
		return projectBuild.toString() + " (" + this.projectBuild.getClass().getName() + ")";
	}

}