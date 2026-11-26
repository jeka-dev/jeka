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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class SourceParser {

    // '@JkDep("value")' or '@JkDep(value = "value")', for the annotations read before compilation
    private static final Pattern ANNOTATION = Pattern.compile("@(" + Stream.of(JkDep.class, JkInjectClasspath.class,
                    JkInjectRunbase.class, JkInjectCompileOption.class, JkCompileOption.class)
            .map(Class::getSimpleName)
            .collect(Collectors.joining("|"))
            + ")\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"");


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

        // Handle JBang notation
        if (rawLine.startsWith("//DEPS ")) {
            String value = JkUtilsString.substringAfterFirst(line, "//DEPS").trim();
            info.addDep(!privateFolder, JkDependency.of(baseDir, value));
            return;
        }
        if (!line.startsWith("@")) {
            return;
        }

        // A line may hold several annotations, e.g. '@JkDep("a:b:1") @JkDep("c:d:2")'
        Matcher matcher = ANNOTATION.matcher(line);
        while (matcher.find()) {
            handleAnnotation(info, matcher.group(1), matcher.group(2), baseDir, privateFolder);
        }
    }

    private static void handleAnnotation(ParsedSourceInfo info, String annotationName, String value, Path baseDir,
                                         boolean privateFolder) {
        if (annotationName.equals(JkDep.class.getSimpleName())
                || annotationName.equals(JkInjectClasspath.class.getSimpleName())) {
            info.addDep(!privateFolder, JkDependency.of(baseDir, value));
        } else if (annotationName.equals(JkInjectRunbase.class.getSimpleName())) {
            if (!JkUtilsString.isBlank(value) && !".".equals(value)) {
                info.importedBaseDirs.add(baseDir.resolve(value));
            }
        } else {  // JkCompileOption or JkInjectCompileOption
            info.compileOptions.addAll(Arrays.asList(JkUtilsString.parseCommandline(value)));
        }
    }

}
