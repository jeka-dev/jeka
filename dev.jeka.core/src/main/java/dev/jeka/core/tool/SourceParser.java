package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.util.Arrays;

class SourceParser {

    private final JkPathTree sources;

    private final Path baseDir;

   private SourceParser(Path baseDir, JkPathTree sources) {
        this.sources = sources;
        this.baseDir = baseDir;
   }

   static SourceParser of(Path baseDir, JkPathTree sources) {
       return new SourceParser(baseDir, sources);
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
        JkUtilsPath.readAllLines(file).forEach(line -> augment(result, line, baseDir, privateFile));
        return result;
    }

    private static boolean isInPrivateFolder(Path file, Path baseDir) {
        Path jekaDef = baseDir.resolve(JkConstants.JEKA_SRC_DIR);
        if (!file.startsWith(jekaDef)) {  // Make testable in Junit
            return false;
        }
        Path relativeToJekaDef = jekaDef.relativize(file);
        Path relativeRoot = relativeToJekaDef.subpath(0, 1);
        return relativeRoot.toString().startsWith("_");
    }

    private static void augment(ParsedSourceInfo info, String rawLine, Path baseDir, boolean privateFolder) {
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
            info.addDep(!privateFolder, CommandLine.toDependency(baseDir, value));
            return;
        }

        // Handle JkInjectRunbase
        annotationParser = new AnnotationParser(line, JkInjectRunbase.class);
        if (annotationParser.isMatching()) {
            String value = annotationParser.readUniqueStringValue();
            info.dependencyProjects.add(baseDir.resolve(value));
            return;
        }

        // Handle JkInjectCompileOption
        annotationParser = new AnnotationParser(line, JkInjectCompileOption.class);
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
            return line.startsWith("@" + annotationClass.getSimpleName());
        }

        String readUniqueStringValue() {
            String afterAnnotation = JkUtilsString.substringAfterFirst(line, "\"");
            return JkUtilsString.substringBeforeFirst(afterAnnotation, "\"");
        }

    }
}
