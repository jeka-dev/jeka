/*
 * Copyright 2014-2025  the original author or authors.
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

package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class KBeanResolution {

    private static final Comparator<Path> PATH_COMPARATOR = (o1, o2) -> {
        if (o1.getNameCount() < o2.getNameCount()) {
            return -1;
        }
        if (o1.getNameCount() > o2.getNameCount()) {
            return 1;
        }
        return o1.compareTo(o2);
    };

    final List<String> allKbeanClassNames;

    final List<String> localKBeanClassNames;

    // fqn of local KBean class. Can be null only if there is no local bean
    final String implicitKBeanClassName;

    //fqn of default KBean class. Can be null
    final String defaultKbeanClassName;

    KBeanResolution(List<String> allKbeanClassNames,
                    List<String> localKBeanClassNames,
                    String defaultKbeanClassName,
                    String implicitKBeanClassName) {
        this.allKbeanClassNames = allKbeanClassNames;
        this.localKBeanClassNames = localKBeanClassNames;
        this.implicitKBeanClassName = implicitKBeanClassName;
        this.defaultKbeanClassName = defaultKbeanClassName;
    }

    static KBeanResolution of(boolean isMasterEngine, JkProperties properties,
                              Path baseDir,
                              List<String> allKbeanClassNames) {

        List<String> localKbeanClassNames = localKBean(baseDir, allKbeanClassNames);
        DefaultAndImplicitKBean defaultAndLocalKBean = DefaultAndImplicitKBean.of(isMasterEngine, properties,
                allKbeanClassNames, localKbeanClassNames);

        return new KBeanResolution(
                allKbeanClassNames,
                localKbeanClassNames,
                defaultAndLocalKBean.defaultKBeanClassName, defaultAndLocalKBean.implicitKbeanClassName
        );
    }

    Optional<String> findKbeanClassName(String kbeanName) {
        if (JkUtilsString.isBlank(kbeanName)) {

            return Optional.empty();
        }
        return this.allKbeanClassNames.stream()
                .filter(className -> KBean.nameMatches(className, kbeanName))
                .findFirst();
    }

    Optional<Class<? extends KBean>> findImplicitKBeanClass() {
        return implicitKBeanClassName == null ? Optional.empty() : Optional.of(JkClassLoader.ofCurrent()
                .load(implicitKBeanClassName));
    }

    private static List<String> localKBean(Path baseDir, List<String> allKbeans) {
        Path jekaSrcClassDir = baseDir.resolve(JkConstants.JEKA_SRC_CLASSES_DIR);
        List<String> localKbeanClassNames = JkPathTree.of(jekaSrcClassDir).streamBreathFirst()
                .excludeDirectories()
                .relativizeFromRoot()
                .filter(path -> path.getFileName().toString().endsWith(".class"))
                .sorted(PATH_COMPARATOR)
                .map(KBeanResolution::classNameFromClassFilePath)
                .filter(allKbeans::contains)
                .collect(Collectors.toList());

        // On IDE, .jeka-work/jeka-src-classes may be not present, so we have to look in jeka-src
        Path jekaSrcDir = baseDir.resolve(JkConstants.JEKA_SRC_DIR);
        if (localKbeanClassNames.isEmpty()) {
            localKbeanClassNames = JkPathTree.of(jekaSrcDir).streamBreathFirst()
                    .excludeDirectories()
                    .relativizeFromRoot()
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(PATH_COMPARATOR)
                    .map(KBeanResolution::classNameFromClassFilePath)
                    .filter(allKbeans::contains)
                    .collect(Collectors.toList());
        }
        return localKbeanClassNames;
    }

    private static String classNameFromClassFilePath(Path relativePath) {
        final String dotName = relativePath.toString().replace('\\', '/').replace('/', '.');
        return JkUtilsString.substringBeforeLast(dotName, ".");
    }
}
