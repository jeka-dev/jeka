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
import dev.jeka.core.api.project.JkDependenciesTxt;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/*
 * There's not nested parent-child structure.
 * All the children must be specified at root level.
 */
class RunbaseGraph {

    private final Node root;

    private final List<Node> children;

    private RunbaseGraph(Node root, List<Node> children) {
        this.root = root;
        this.children = children;
    }

    static RunbaseGraph of(Class<? extends KBean> parentKBean, JkRunbase runbase) {

        // child bases
        List<Node> children = getChildPaths(runbase).stream()
                .map(Node::getOrCreate)
                .collect(Collectors.toList());

        // injected bases for root node
        List<Node> rootInjectedNodes = getInjectedPaths(parentKBean, runbase).stream()
                .map(Node::getOrCreate)
                .collect(Collectors.toList());

        // The Node.ALL is presumed sorted
        List<Node> sortedChildren = new LinkedList<>();
        for (Node node : Node.ALL) {
            if (children.contains(node)) {
                sortedChildren.add(node);
            }
        }

        Node root = new Node(runbase, rootInjectedNodes);
        return new RunbaseGraph(root, sortedChildren);
    }

    List<JkRunbase> getChildren() {
        return children.stream().map(Node::getRunbase).collect(Collectors.toList());
    }

    boolean declaresChildren() {
        return !this.children.isEmpty();
    }

    JkRunbase getRunbase(String baseDirRelPath) {
        Node node = getNode(baseDirRelPath);
        return node.runbase;
    }

    List<JkRunbase> getInjectedRunbases(String baseDirRelPath) {
        Node theNode = getNode(baseDirRelPath);
        return theNode.getInjectedNodes().stream()
                .map(Node::getRunbase)
                .collect(Collectors.toList());
    }

    static List<Path> getChildBaseProps(Path parentBaseDir, JkProperties properties) {
        String childBasesProp = properties.get(JkConstants.JEKA_CHILD_BASES_PROP);
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

    private Node getNode(String baseDirRelPath) {
        return Node.ALL.stream()
                .filter(node -> root.runbase.getBaseDir().resolve(baseDirRelPath).normalize()
                        .equals(node.runbase.getBaseDir()))
                .findFirst()
                .orElseThrow(() -> new JkException("No runbase '%s' found among %s.",
                        root.runbase.getBaseDir().resolve(baseDirRelPath).normalize(), childBaseDirs()));
    }

    private List<Path> childBaseDirs() {
        return children.stream().map(node -> node.runbase.getBaseDir()).collect(Collectors.toList());
    }

    // Only the base dir is set at init time, the values are populated afterward
    private static class Node implements Comparable<Node> {

        private final static LinkedHashSet<Node> ALL = new LinkedHashSet<>();

        private final JkRunbase runbase;

        private final List<Node> injectedNodes;

        private static Node getOrCreate(Path baseDir) {
            return ALL.stream()
                    .filter(node -> node.runbase.getBaseDir().equals(baseDir.toAbsolutePath()))
                    .findFirst()
                    .orElseGet(() -> createFrom(baseDir));
        }

        private static Node createFrom(Path baseDir) {
            JkUtilsAssert.argument(baseDir.isAbsolute(), "baseDir must be absolute");
            JkRunbase runbase = createRunbase(baseDir);
            List<Node> injectedNodes = getInjectedPaths(null, runbase).stream()
                            .map(Node::getOrCreate)
                            .collect(Collectors.toList());
            return new Node(runbase, injectedNodes);
        }

        // Only used for the root node
        private Node(JkRunbase runbase, List<Node> injectedNodes) {
            this.runbase = runbase;
            this.injectedNodes = injectedNodes;
            ALL.add(this);
        }

        private JkRunbase getRunbase() {
            return runbase;
        }

        private List<Node> getInjectedNodes() {
            return injectedNodes;
        }

        private static Optional<Node> find(JkRunbase searchedRunbase) {
            return Node.ALL.stream()
                    .filter(node -> searchedRunbase.getBaseDir().toAbsolutePath().equals(node.runbase.getBaseDir()))
                    .findFirst();
        }

        private static JkRunbase createRunbase(Path baseDir) {
            Engine engine = Engines.get(baseDir);
            engine.resolveKBeans();
            ClassLoader augmentedClassloader = JkUrlClassLoader.of(engine.getClasspathSetupResult().runClasspath).get();
            Thread.currentThread().setContextClassLoader(augmentedClassloader);
            return engine.getOrCreateRunbase(new KBeanAction.Container(), false);
        }

        @Override
        public int compareTo(Node other) {
            return 0;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Node)) return false;

            Node node = (Node) o;
            return runbase.getBaseDir().equals(node.runbase.getBaseDir());
        }

        @Override
        public int hashCode() {
            return runbase.getBaseDir().hashCode();
        }

        @Override
        public String toString() {
            return this.runbase.getBaseDir().getFileName().toString();
        }
    }

    private static List<Path> getChildPaths(JkRunbase runbase) {
        List<Path> result = getChildBaseProps(runbase);
        List<Path> distinctResult = result.stream().distinct().collect(Collectors.toList());
        JkLog.debug("Found child bases for %s: %s", runbase.getBaseDir(), distinctResult);
        return distinctResult;
    }

    private static List<Path> getInjectedPaths(Class<? extends KBean> parentKBeanClass, JkRunbase runbase) {
        Path parentBaseDir = runbase.getBaseDir();
        List<Path> result = new LinkedList<>();
        if (parentKBeanClass != null) {
            Arrays.stream(parentKBeanClass.getDeclaredFields())
                    .map(field -> field.getAnnotation(JkInject.class))
                    .filter(Objects::nonNull)
                    .map(JkInject::value)
                    .filter(value -> !value.isEmpty())
                    .map(parentBaseDir::resolve)
                    .map(Path::normalize)
                    .forEach(result::add);
        }
        result.addAll(JkDependenciesTxt.getModuleDependencies(parentBaseDir));
        List<Path> distinctResult = result.stream().distinct().collect(Collectors.toList());
        JkLog.debug("Found injected bases for %s: %s", runbase.getBaseDir(), distinctResult);
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
        return getChildBaseProps(runbase.getBaseDir(), runbase.getProperties());
    }

}
