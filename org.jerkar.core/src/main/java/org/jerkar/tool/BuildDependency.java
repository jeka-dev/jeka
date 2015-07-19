package org.jerkar.tool;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jerkar.api.depmanagement.JkComputedDependency;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsString;

/**
 * A dependency on files and Jerkar build definition of external project.
 * This class is particularly used in multi-project though this is rarely directly used by the
 * build definition authors.
 * 
 * @author Jerome Angibaud
 */
final class BuildDependency extends JkComputedDependency {

	private static final long serialVersionUID = 1L;

	private final JkBuild projectBuild;

	private final List<String> methods;


	private BuildDependency(JkBuild projectBuild, List<String> methods, Set<File> files) {
		super(runnable(projectBuild, methods), files);
		this.methods = methods;
		this.projectBuild = projectBuild;
	}

	private static Runnable runnable(final JkBuild build, final List<String> methods) {
		return new Runnable() {

			@Override
			public void run() {
				for (final String method : methods) {
					JkUtilsReflect.invoke(build, method);
				}
			}
		};
	}

	public static BuildDependency of(JkBuild projectBuild, Set<File> files) {
		return of(projectBuild, JkConstants.DEFAULT_METHOD, files);
	}

	public static BuildDependency of(JkBuild projectBuild, String methods, Set<File> files) {
		final List<String> list = Arrays.asList(JkUtilsString.split(methods, " "));
		return new BuildDependency(projectBuild, list, new HashSet<File>(files));
	}

	public static BuildDependency of(JkBuild projectBuild, File... files) {
		return of(projectBuild, new HashSet<File>(Arrays.asList(files)));
	}

	public static BuildDependency of(JkBuild projectBuild, String methods, File... files) {
		return of(projectBuild, methods, new HashSet<File>(Arrays.asList(files)));
	}


	public JkBuild projectBuild() {
		return projectBuild;
	}


	@Override
	public String toString() {
		return projectBuild.toString() + " (" + this.projectBuild.getClass().getName() + " " + methods+")";
	}

}