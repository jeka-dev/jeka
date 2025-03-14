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

package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

final class SourceParser {

    private final JkPathTree sources;

    private final Path baseDir;

    private SourceParser(Path baseDir, JkPathTree sources) {
        this.sources = sources;
        this.baseDir = baseDir;
    }

    static SourceParser of(Path baseDir, JkPathTree sources) {
        return new SourceParser(baseDir, sources);
    }

    static SourceParser of(Path baseDir) {
        JkPathTree sources = JkPathTree.of(baseDir.resolve(JkConstants.JEKA_SRC_DIR))
               .andMatcher(Engine.JAVA_OR_KOTLIN_SOURCE_MATCHER);
        return of(baseDir,sources);
    }

    ParsedSourceInfo parse() {
        return sources.stream()
                .map(path -> parse(path, baseDir))
                .reduce(new ParsedSourceInfo(), ParsedSourceInfo::merge);
    }

    // non-private for testing purpose
    static ParsedSourceInfo parse(Path file, Path baseDir) {
        ParsedSourceInfo result = new ParsedSourceInfo();
        boolean privateFile = isInPrivateFolder(file, baseDir);
        JkUtilsPath.lines(file, StandardCharsets.UTF_8).forEach(line -> augment(result, line, baseDir, privateFile));
        return result;
    }

    private static boolean isInPrivateFolder(Path file, Path baseDir) {
        Path jekaSrc = baseDir.resolve(JkConstants.JEKA_SRC_DIR);
        if (!file.startsWith(jekaSrc)) {  // Make testable in Junit
            return false;
        }
        Path relativeToJekaSrc = jekaSrc.relativize(file);
        Path relativeRoot = relativeToJekaSrc.subpath(0, 1);
        return relativeRoot.toString().startsWith("_");
    }

    private static void augment(ParsedSourceInfo info, String rawLine, Path baseDir, boolean privateFolder) {
        String line = rawLine.trim();
        if (!line.startsWith("@") && !rawLine.startsWith("//DEPS ")) {
            return;
        }

        // Handle JBang notation
        if (line.startsWith("//DEPS ")) {
            String value = JkUtilsString.substringAfterFirst(line, "//DEPS").trim();
            info.addDep(!privateFolder, JkDependency.of(baseDir, value));
            return;
        }

        // Handle @JkInjectClasspath
        AnnotationParser annotationParser = new AnnotationParser(line, JkInjectClasspath.class);
        if (annotationParser.isMatching()) {
            String value = annotationParser.readUniqueStringValue();
            info.addDep(!privateFolder, JkDependency.of(baseDir, value));
            return;
        }

        // Handle @JkInjectClasspath
        annotationParser = new AnnotationParser(line, JkDep.class);
        if (annotationParser.isMatching()) {
            String value = annotationParser.readUniqueStringValue();
            info.addDep(!privateFolder, JkDependency.of(baseDir, value));
            return;
        }

        // Handle @JkInjectRunbase
        annotationParser = new AnnotationParser(line, JkInjectRunbase.class);
        if (annotationParser.isMatching()) {
            String value = annotationParser.readUniqueStringValue();
            if (!JkUtilsString.isBlank(value) && !".".equals(value)) {
                info.importedBaseDirs.add(baseDir.resolve(value));
            }
            return;
        }

        // Handle @JkInject
        annotationParser = new AnnotationParser(line, JkInject.class);
        if (annotationParser.isMatching()) {
            String value = annotationParser.readUniqueStringValue();
            if (!JkUtilsString.isBlank(value) && !".".equals(value)) {
                info.importedBaseDirs.add(baseDir.resolve(value));
            }
            return;
        }

        // Handle @JkSubBase
        annotationParser = new AnnotationParser(line, JkSubBase.class);
        if (annotationParser.isMatching()) {
            String value = annotationParser.readUniqueStringValue();
            List<Path> subBaseDirs = findBaseDirs(value, baseDir);
            info.importedBaseDirs.addAll(subBaseDirs);
        }

        // Handle @JkInjectCompileOption
        annotationParser = new AnnotationParser(line, JkInjectCompileOption.class);
        if (annotationParser.isMatching()) {
            String value = annotationParser.readUniqueStringValue();
            info.compileOptions.addAll(Arrays.asList(JkUtilsString.parseCommandline(value)));
        }

        // Handle JkInjectCompileOption
        annotationParser = new AnnotationParser(line, JkCompileOption.class);
        if (annotationParser.isMatching()) {
            String value = annotationParser.readUniqueStringValue();
            info.compileOptions.addAll(Arrays.asList(JkUtilsString.parseCommandline(value)));
        }
    }

    static class AnnotationParser {

        private final String line;

        private final Class<?> annotationClass;

        AnnotationParser(String line, Class<?> annotationClass) {
            this.line = line;
            this.annotationClass = annotationClass;
        }

        boolean isMatching() {
            return line.startsWith("@" + annotationClass.getSimpleName() + "(");
        }

        String readUniqueStringValue() {
            String afterAnnotation = JkUtilsString.substringAfterFirst(line, "\"");
            return JkUtilsString.substringBeforeFirst(afterAnnotation, "\"");
        }

    }

    private static List<Path> findBaseDirs(String dirPath, Path baseDir) {
        if (dirPath.endsWith("*")) {
            String parentDirString = dirPath.equals("*") ? "" : JkUtilsString.substringBeforeLast(dirPath, "/*");
            Path parentDir = baseDir.resolve(parentDirString);
            return JkUtilsPath.listDirectChildren(parentDir).stream()
                    .filter(JkRunbase::isJekaProject)
                    .collect(Collectors.toList());
        }
        Path candidate = baseDir.resolve(dirPath);
        if (!Files.isDirectory(candidate) ) {
            return Collections.singletonList(candidate);
        }
        throw new JkException("@JkSubBase(\"%s\") mentions a directectory that does not exist (base-dir=%s).",
                candidate, baseDir);
    }
}
