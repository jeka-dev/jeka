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
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

class RunbaseGraph {

    private final List<Node> nodes;

    private final Path parentBaseDir;

    private RunbaseGraph(List<Node> nodes, Path parentBaseDir) {
        this.nodes = nodes;
        this.parentBaseDir = parentBaseDir;
    }

    static RunbaseGraph of(Class<? extends KBean> parentKBean, Path parentBaseDir) {
        List<Node> nodes = getChildPaths(parentKBean, parentBaseDir).stream()
                .map(Node::new)
                .collect(Collectors.toList());
        return new RunbaseGraph(nodes, parentBaseDir);
    }

    static RunbaseGraph empty() {
        return new RunbaseGraph(Collections.emptyList(), null);
    }

    List<JkRunbase> getRunbases() {
        return nodes.stream().map(Node::get).collect(Collectors.toList());
    }

    JkRunbase getRunbase(String baseDirRelPath) {
        return nodes.stream()
                .filter(node -> parentBaseDir.resolve(baseDirRelPath).normalize().equals(node.baseDir))
                .map(Node::get)
                .findFirst()
                .orElseThrow(() -> new JkException("No runbase '%s' found among %s.",
                        parentBaseDir.resolve(baseDirRelPath).normalize(), childBaseDirs()));
    }

    private List<Path> childBaseDirs() {
        return nodes.stream().map(node -> node.baseDir).collect(Collectors.toList());
    }

    private static class Node {

        private JkRunbase runbase;

        private final Path baseDir;

        private Node(Path baseDir) {
            this.baseDir = baseDir;
        }

        JkRunbase get() {
            if (runbase != null) {
                return runbase;
            }
            JkLog.startTask("init-runbase: " + baseDir);
            Engine engine = Engines.get(baseDir);
            engine.resolveKBeans();
            ClassLoader augmentedClassloader = JkUrlClassLoader.of(engine.getClasspathSetupResult().runClasspath).get();
            Thread.currentThread().setContextClassLoader(augmentedClassloader);
            this.runbase = engine.getOrCreateRunbase(new KBeanAction.Container());
            JkLog.endTask();
            return runbase;
        }
    }

    private static List<Path> getChildPaths(Class<? extends KBean> parentKBeanClass, Path parentBaseDir) {
        List<Path> result = new LinkedList<>();
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
        return result.stream().distinct().collect(Collectors.toList());
    }

    private static List<Path> findBaseDirs(String dirPath, Path baseDir) {
        if (dirPath.endsWith("*")) {
            String parentDirString = dirPath.equals("*") ? "" : JkUtilsString.substringBeforeLast(dirPath, "/*");
            Path parentDir = baseDir.resolve(parentDirString);
            return JkUtilsPath.listDirectChildren(parentDir).stream()
                    .filter(JkRunbase::isJekaProject)
                    .collect(Collectors.toList());
        }
        Path candidate = baseDir.resolve(dirPath).normalize();
        if (Files.isDirectory(candidate) ) {
            return Collections.singletonList(candidate);
        }
        throw new JkException("@JkSubBase(\"%s\") mentions a directory that does not exist (base-dir=%s).",
                candidate, baseDir);
    }

}
