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

package dev.jeka.core.api.java.embedded.shade;

import java.lang.module.ModuleFinder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Determines what to relocate in a shaded jar: only the packages coming from the dependencies,
 * and neither those of the application nor those of the JDK, even if they share the same root.
 * <p>
 * Packages are expressed as paths, as <code>org/slf4j/spi</code>.
 */
class ShadedPackages {

    private static final Pattern MULTI_RELEASE_PREFIX = Pattern.compile("^META-INF/versions/\\d+/");

    private final Set<String> packages;

    // Parents of relocated packages having no file by themselves, as 'com/google'.
    // They may be mentioned as a prefix in string constants.
    private final Set<String> parents = new HashSet<>();

    private ShadedPackages(Set<String> packages, Set<String> keptPackages) {
        this.packages = packages;
        packages.forEach(pack -> addParents(pack, parents));
        Set<String> keptParents = new HashSet<>();
        keptPackages.forEach(pack -> addParents(pack, keptParents));
        parents.removeAll(keptPackages);
        parents.removeAll(keptParents);
        parents.removeIf(parent -> !parent.contains("/"));
    }

    /**
     * @param appEntries Names of the files contained in the application jar.
     * @param dependencyEntries Names of the files contained in the dependency jars.
     */
    static ShadedPackages of(Collection<String> appEntries, Collection<String> dependencyEntries) {
        Set<String> kept = jdkPackages();
        kept.addAll(classPackages(appEntries));

        // Resources located under the root folder of a class are relocated as well.
        Set<String> classPackages = classPackages(dependencyEntries);
        Set<String> roots = classPackages.stream().map(ShadedPackages::root).collect(Collectors.toSet());
        Set<String> packages = dependencyEntries.stream()
                .map(ShadedPackages::withoutVersion)
                .map(ShadedPackages::parent)
                .filter(pack -> roots.contains(root(pack)))
                .collect(Collectors.toSet());
        packages.removeAll(kept);
        return new ShadedPackages(packages, kept);
    }

    /**
     * Returns the root folders of the packages to relocate.
     */
    Set<String> roots() {
        return packages.stream().map(ShadedPackages::root).collect(Collectors.toSet());
    }

    /**
     * Returns <code>true</code> if the specified class, resource or package path must be relocated.
     */
    boolean contains(String path) {
        String name = path.startsWith("/") ? path.substring(1) : path;
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        return packages.contains(name) || packages.contains(parent(name)) || parents.contains(name);
    }

    private static Set<String> classPackages(Collection<String> entries) {
        return entries.stream()
                .filter(entry -> entry.endsWith(".class"))
                .map(ShadedPackages::withoutVersion)
                .map(ShadedPackages::parent)
                .filter(pack -> !pack.isEmpty())
                .filter(pack -> !"META-INF".equals(root(pack)))
                .collect(Collectors.toSet());
    }

    private static Set<String> jdkPackages() {
        // Other modules, as JavaFX, may be bundled in some JDKs
        return ModuleFinder.ofSystem().findAll().stream()
                .map(module -> module.descriptor())
                .filter(module -> module.name().startsWith("java.") || module.name().startsWith("jdk."))
                .flatMap(module -> module.packages().stream())
                .map(pack -> pack.replace('.', '/'))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static void addParents(String pack, Set<String> result) {
        for (String parent = parent(pack); !parent.isEmpty(); parent = parent(parent)) {
            result.add(parent);
        }
    }

    private static String withoutVersion(String entry) {
        return MULTI_RELEASE_PREFIX.matcher(entry).replaceFirst("");
    }

    private static String parent(String path) {
        int index = path.lastIndexOf('/');
        return index < 0 ? "" : path.substring(0, index);
    }

    private static String root(String path) {
        int index = path.indexOf('/');
        return index < 0 ? path : path.substring(0, index);
    }

}
