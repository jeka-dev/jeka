package org.jerkar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jerkar.api.java.JkClassLoader;
import org.junit.Assert;
import org.junit.Test;

import jdepend.framework.JDepend;
import jdepend.framework.JavaClass;
import jdepend.framework.JavaPackage;
import jdepend.framework.PackageFilter;

@SuppressWarnings("javadoc")
public class PackageDependency {

    @Test
    public void testDependencies() throws IOException {
        final String packagePrefix = "org.jerkar";
        final File classDir = JkClassLoader.current().fullClasspath().getEntryContainingClass("org.jerkar.tool.Main");
        final String cycle = PackageAnalyser.of(classDir, packagePrefix).cycle();
        Assert.assertTrue(cycle, cycle == null);
    }

    public static class PackageAnalyser {

        private final Collection<JavaPackage> packages;

        private PackageAnalyser(Collection<JavaPackage> packages) {
            this.packages = packages;
        }

        @SuppressWarnings("unchecked")
        public static PackageAnalyser of(File classDir, final String packagePrefix) {
            final PackageFilter filter = new PackageFilter() {

                @Override
                public boolean accept(String name) {
                    return name.startsWith(packagePrefix);
                }

            };
            final JDepend depend = new JDepend(filter);
            try {
                depend.addDirectory(classDir.getPath());
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            final Collection<JavaPackage> packages = depend.analyze();
            return new PackageAnalyser(packages);
        }

        public String cycle() {
            for (final JavaPackage javaPackage : packages) {
                if (javaPackage.containsCycle()) {
                    return "package " + javaPackage.getName() + " involved in cycles : " + cycleAsString(javaPackage);
                }
            }
            return null;
        }

        private static String cycleAsString(JavaPackage root) {
            final List<JavaPackage> involvedPackages = new ArrayList<JavaPackage>();
            root.collectCycle(involvedPackages);
            final StringBuilder builder = new StringBuilder();
            for (final JavaPackage involvedPackage : involvedPackages) {
                builder.append(involvedPackage.getName()).append(" -> ");
            }
            if (builder.length() > 4) {
                builder.delete(builder.length() - 4, builder.length());
            }
            builder.append("\n");

            for (int i = 0; i < involvedPackages.size()-1; i++) {
                final JavaPackage pack = involvedPackages.get(i);
                final JavaPackage nextPack = involvedPackages.get(i+1);
                builder.append("\nInvolved classes of " + pack.getName()).append("\n");
                builder.append("------------------------------------------\n");
                for (final Object javaClassObject : pack.getClasses()) {
                    final JavaClass javaClass = (JavaClass) javaClassObject;
                    for (final Object importedPackageObject : javaClass.getImportedPackages()) {
                        final JavaPackage importedPackage = (JavaPackage) importedPackageObject;
                        if (nextPack.getName().startsWith(importedPackage.getName())) {
                            builder.append(javaClass.getName()).append("\n");
                        }
                    }
                }
            }
            return builder.toString();
        }

    }

}