package org.jerkar;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import jdepend.framework.JDepend;
import jdepend.framework.JavaPackage;
import jdepend.framework.PackageFilter;

import org.jerkar.api.java.JkClassLoader;
import org.junit.Assert;
import org.junit.Test;

public class PackageDependency {

	@SuppressWarnings("unchecked")
	@Test
	public void testDependencies() throws IOException {
		final String packageFilter = "org.jerkar.api";
		final PackageFilter filter = new PackageFilter() {

			@Override
			public boolean accept(String name) {
				return name.startsWith(packageFilter);
			}

		};
		final JDepend depend = new JDepend(filter);
		final File classDir = JkClassLoader.current().fullClasspath().getEntryContainingClass("org.jerkar.tool.Main");
		System.out.println(classDir.getAbsolutePath());
		depend.addDirectory(classDir.getPath());
		final Collection<JavaPackage> packages = depend.analyze();
		for (final JavaPackage javaPackage : packages) {
			if (javaPackage.getName().startsWith(packageFilter)) {
				Assert.assertFalse("package " + javaPackage.getName() + " involved in cycles : "
						+ packageCycle(javaPackage)
						, javaPackage.containsCycle());
			}

		}
		System.out.println("--------------------" + depend.containsCycles());
		Assert.assertFalse("package cycles", depend.containsCycles());
	}

	private static String packageCycle(JavaPackage javaPackage) {
		final List<JavaPackage> objects = new LinkedList<JavaPackage>();
		javaPackage.collectCycle(objects);
		final StringBuilder builder = new StringBuilder();
		for (final JavaPackage javaPackage2 : objects) {
			builder.append(javaPackage2.getName()).append(" -> ");
		}
		if (builder.length() > 4) {
			builder.delete(builder.length()-4, builder.length());
			System.out.println(builder);
		} else {

		}

		return builder.toString();
	}

}
