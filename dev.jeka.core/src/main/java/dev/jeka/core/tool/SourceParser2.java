package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.util.Arrays;

class SourceParser2 {

    private final JkPathTree<?> sources;

    private final Path baseDir;

   private SourceParser2(Path baseDir, JkPathTree<?> sources) {
        this.sources = sources;
        this.baseDir = baseDir;
   }

   static SourceParser2 of(Path baseDir, JkPathTree<?> sources) {
       return new SourceParser2(baseDir, sources);
   }


    ParsedSourceInfo parse() {
        return sources.stream()
                .map(path -> parse(path, baseDir))
                .reduce(new ParsedSourceInfo(), ParsedSourceInfo::merge);
    }

    // non-private for tesing purpose
    static ParsedSourceInfo parse(Path file, Path baseDir) {
        ParsedSourceInfo result = new ParsedSourceInfo();
        JkUtilsPath.readAllLines(file).forEach(line -> augment(result, line, baseDir));
        return result;
    }

    private static void augment(ParsedSourceInfo info, String rawLine, Path baseDir) {
        if (!rawLine.contains("@")) {
            return; // fast return
        }
        String line = rawLine.trim();
        if (!line.startsWith("@")) {
            return;
        }

        // Handle JkInjectClasspath
        AnnotationParser annotationParser = new AnnotationParser(line, JkInjectClasspath.class);
        if (annotationParser.isMatching()) {
            String value = annotationParser.readUniqueStringValue();
            info.addDep(CommandLine.toDependency(baseDir, value));
            return;
        }

        // Handle JkInjectProject
        annotationParser = new AnnotationParser(line, JkInjectProject.class);
        if (annotationParser.isMatching()) {
            String value = annotationParser.readUniqueStringValue();
            info.dependencyProjects.add(baseDir.resolve(value));
            return;
        }

        // Handle JkInjectCompileOption
        annotationParser = new AnnotationParser(line, JkInjectCompileOption.class);
        if (annotationParser.isMatching()) {
            String value = annotationParser.readUniqueStringValue();
            info.compileOptions.addAll(Arrays.asList(JkUtilsString.translateCommandline(value)));
        }

    }

    private static boolean isDeclaringAnnotation(String candidate, Class<?> annotationClass) {
        return candidate.startsWith("@" + annotationClass.getSimpleName());
    }

    static class AnnotationParser {

        private final String line;

        private final Class<?> annotationClass;

        AnnotationParser(String line, Class<?> annotationClass) {
            this.line = line;
            this.annotationClass = annotationClass;
        }

        boolean isMatching() {
            return line.startsWith("@" + annotationClass.getSimpleName());
        }

        String readUniqueStringValue() {
            String afterAnnotation = JkUtilsString.substringAfterFirst(line, "\"");
            return JkUtilsString.substringBeforeFirst(afterAnnotation, "\"");
        }

    }
}
