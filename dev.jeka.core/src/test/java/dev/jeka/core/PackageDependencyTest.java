package dev.jeka.core;

import dev.jeka.core.tool.Main;
import dev.jeka.core.wrapper.Booter;
import jdepend.framework.JDepend;
import jdepend.framework.JavaClass;
import jdepend.framework.JavaPackage;
import jdepend.framework.PackageFilter;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("javadoc")
public class PackageDependencyTest {

    @Test
    public void testDependencies() throws Exception {
        final String packagePrefix = "dev.jeka.core";
        final File classDir = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();
        PackageAnalyser packageAnalyser = PackageAnalyser.of(classDir, packagePrefix);
        final String cycle = packageAnalyser.cycle();
        assertTrue(cycle, cycle == null);
    }

    public static class PackageAnalyser {

        private final Collection<JavaPackage> packages;

        private PackageAnalyser(Collection<JavaPackage> packages) {
            this.packages = packages;
        }

        @SuppressWarnings("unchecked")
        public static PackageAnalyser of(File classDir, final String packagePrefix) throws IOException {
            final PackageFilter filter = new PackageFilter() {

                @Override
                public boolean accept(String name) {
                    return name.startsWith(packagePrefix);
                }

            };
            final JDepend depend = new JDepend(filter);
            depend.addDirectory(classDir.getPath());
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
            final List<JavaPackage> involvedPackages = new ArrayList<>();
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
                builder.append("\nInvolved classes of ").append(pack.getName()).append("\n");
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

    @Test
    public void testWrapperHasNoImport() throws Exception {
        final String packageName = "dev.jeka.core.wrapper";
        final File classDir = Paths.get(Booter.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();
        final JDepend jDepend = new JDepend(new PackageFilter() {
            @Override
            public boolean accept(String packageName) {
                return packageName.startsWith("dev.jeka.core.");
            }
        });
        jDepend.addPackage(packageName);
        jDepend.addDirectory(classDir.getPath());
        Collection<JavaPackage> javaPackages = jDepend.analyze();
        JavaPackage wrapper = javaPackages.stream()
                .peek(javaPackage -> System.out.println(javaPackage.getName()))
                .filter(javaPackage -> javaPackage.getName().equals(packageName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(packageName +  " not found"));
        assertTrue("wrapper.Booter class has dependencies on other Jeka packages", wrapper.getEfferents().isEmpty());
    }

}