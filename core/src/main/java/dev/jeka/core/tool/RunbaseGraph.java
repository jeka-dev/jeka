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

import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

class RunbaseGraph {

    private final List<LazyNode> children;

    private final JkRunbase runbase;

    private final boolean declaresChildren;

    private RunbaseGraph(List<LazyNode> children, JkRunbase runbase, boolean declaresChildren) {
        this.children = children;
        this.runbase = runbase;
        this.declaresChildren = declaresChildren;
    }

    static RunbaseGraph of(Class<? extends KBean> parentKBean, JkRunbase runbase) {
        List<LazyNode> nodes = getChildPaths(parentKBean, runbase).stream()
                .map(LazyNode::of)
                .collect(Collectors.toList());
        LazyNode.find(runbase).ifPresent(lazyNode -> lazyNode.populate(runbase, nodes));
        boolean declaresChildren = isDeclaringChildren(parentKBean, runbase);
        return new RunbaseGraph(nodes, runbase, declaresChildren);
    }

    static RunbaseGraph empty() {
        return new RunbaseGraph(Collections.emptyList(), null, false);
    }

    List<JkRunbase> getOrInitRunbases() {
        LinkedHashSet<LazyNode> sortedNodes = new LinkedHashSet<>();
        for (LazyNode node : children) {
            fill(sortedNodes, node);
        }
        return sortedNodes.stream().map(LazyNode::getRunbase).collect(Collectors.toList());
    }

    private static void fill(Collection<LazyNode> recipient, LazyNode node) {
        for (LazyNode child : node.children) {
            fill(recipient, child);
        }
        recipient.add(node);
    }

    boolean declaresChildren() {
        return declaresChildren;
    }

    JkRunbase getRunbase(String baseDirRelPath) {
        return LazyNode.ALL.stream()
                .filter(node -> runbase.getBaseDir().resolve(baseDirRelPath).normalize().equals(node.baseDir))
                .map(LazyNode::getRunbase)
                .findFirst()
                .orElseThrow(() -> new JkException("No runbase '%s' found among %s.",
                        runbase.getBaseDir().resolve(baseDirRelPath).normalize(), childBaseDirs()));
    }

    private List<Path> childBaseDirs() {
        return children.stream().map(node -> node.baseDir).collect(Collectors.toList());
    }

    private static class LazyNode implements Comparable<LazyNode> {

        private final static LinkedHashSet<LazyNode> ALL = new LinkedHashSet<>();

        private JkRunbase runbase;

        private final Path baseDir;

        private List<LazyNode> children = new LinkedList<>();

        private static LazyNode of(Path baseDir) {
            return ALL.stream()
                    .filter(node -> node.baseDir.equals(baseDir.toAbsolutePath()))
                    .findFirst()
                    .orElseGet(() -> new LazyNode(baseDir));
        }

        private LazyNode(Path baseDir) {
            JkUtilsAssert.argument(baseDir.isAbsolute(), "baseDir must be absolute");
            this.baseDir = baseDir;
            ALL.add(this);
        }

        private static Optional<LazyNode> find(JkRunbase searchedRunbase) {
            return LazyNode.ALL.stream()
                    .filter(node -> searchedRunbase.getBaseDir().toAbsolutePath().equals(node.baseDir))
                    .findFirst();
        }

        JkRunbase getRunbase() {
            if (runbase != null) {
                return runbase;
            }
            Engine engine = Engines.get(baseDir);
            engine.resolveKBeans();
            ClassLoader augmentedClassloader = JkUrlClassLoader.of(engine.getClasspathSetupResult().runClasspath).get();
            Thread.currentThread().setContextClassLoader(augmentedClassloader);
            this.runbase = engine.getOrCreateRunbase(new KBeanAction.Container(), false);
            return this.runbase;
        }

        void populate(JkRunbase runbase, List<LazyNode> children) {
            this.runbase = runbase;
            this.children = children;
        }

        @Override
        public int compareTo(LazyNode other) {
            return 0;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof LazyNode)) return false;

            LazyNode node = (LazyNode) o;
            return baseDir.equals(node.baseDir);
        }

        @Override
        public int hashCode() {
            return baseDir.hashCode();
        }

        @Override
        public String toString() {
            return this.baseDir.getFileName().toString();
        }
    }

    private static boolean isDeclaringChildren(Class<? extends KBean> parentKBeanClass, JkRunbase runbase) {
        if (parentKBeanClass != null && parentKBeanClass.getDeclaredAnnotationsByType(JkChildBase.class).length > 0) {
            return true;
        }
        return !getChildBaseProps(runbase).isEmpty();
    }

    private static List<Path> getChildPaths(Class<? extends KBean> parentKBeanClass, JkRunbase runbase) {
        Path parentBaseDir = runbase.getBaseDir();
        List<Path> result = new LinkedList<>();
        if (parentKBeanClass != null) {
            Arrays.stream(parentKBeanClass.getDeclaredAnnotationsByType(JkChildBase.class))
                    .map(JkChildBase::value)
                    .filter(Objects::nonNull)
                    .flatMap(value -> findBaseDirs(value, parentBaseDir).stream())
                    .map(parentBaseDir::resolve)
                    .map(Path::normalize)
                    .forEach(result::add);
            Arrays.stream(parentKBeanClass.getDeclaredFields())
                    .map(field -> field.getAnnotation(JkInject.class))
                    .filter(Objects::nonNull)
                    .map(JkInject::value)
                    .filter(value -> !value.isEmpty())
                    .map(parentBaseDir::resolve)
                    .map(Path::normalize)
                    .forEach(result::add);
        }
        result.addAll(getChildBaseProps(runbase));

        List<Path> distinctResult = result.stream().distinct().collect(Collectors.toList());
        JkLog.debug("Found child bases for %s: %s", runbase.getBaseDir(), distinctResult);
        return distinctResult;
    }

    private static List<Path> findBaseDirs(String dirPath, Path baseDir) {
        if (dirPath.endsWith("*")) {
            String parentDirString = dirPath.equals("*") ? "" : JkUtilsString.substringBeforeLast(dirPath, "/*");
            Path parentDir = baseDir.resolve(parentDirString);
            return JkUtilsPath.listDirectChildren(parentDir).stream()
                    .filter(JkLocator::isJekaProject)
                    .collect(Collectors.toList());
        }
        Path candidate = baseDir.resolve(dirPath).normalize();
        if (Files.isDirectory(candidate) ) {
            return Collections.singletonList(candidate);
        }
        throw new JkException("@JkSubBase(\"%s\") mentions a directory that does not exist (base-dir=%s).",
                candidate, baseDir);
    }

    private static List<Path> getChildBaseProps(JkRunbase runbase) {
        Path parentBaseDir = runbase.getBaseDir();
        String childBasesProp = runbase.getProperties().get(JkConstants.JEKA_CHILD_BASES_PROP);
        if (!JkUtilsString.isBlank(childBasesProp)) {
            return Arrays.stream(childBasesProp.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .flatMap(value -> findBaseDirs(value, parentBaseDir).stream())
                    .map(parentBaseDir::resolve)
                    .map(Path::normalize)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

}
