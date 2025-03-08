/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core;

import jdepend.framework.JDepend;
import jdepend.framework.JavaClass;
import jdepend.framework.JavaPackage;
import jdepend.framework.PackageFilter;
import org.apache.ivy.Main;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;


/*
 * Unit test failing if a cyclic dependency has been detected in the jeka-core code base.
 */
class PackageCyclicDependencyTest {

    @Test
    void codeBase_hasNoCyclicDependency() throws Exception {
        final String packagePrefix = "dev.jeka.core";
        final File classDir = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();
        PackageAnalyser packageAnalyser = PackageAnalyser.of(classDir, packagePrefix);
        final String cycle = packageAnalyser.cycle();
        assertNull(cycle, cycle);
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

}