package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleDependency;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/*
 * Without doubt, the most crappy code of this project.
 * The important point is that we achieve parsing without using any dependencies : just the JDK.
 *
 * @author Jerome Angibaud
 */
final class SourceParser {

    private static SourceParser of(Path baseDir, Path code) {
        return of(baseDir, JkUtilsPath.toUrl(code));
    }

    public static SourceParser of(Path baseDir, Iterable<Path>  files) {
        SourceParser result = new SourceParser(JkDependencySet.of(), JkRepoSet.of(),
                new LinkedList<>(), new LinkedList<>());
        for (final Path code : files) {
            result = result.and(of(baseDir, code));
        }
        return result;
    }

    static SourceParser of(Path baseDir, URL codeUrl) {
        try (final InputStream inputStream = JkUtilsIO.inputStream(codeUrl)) {
            final String uncomentedCode = removeComments(inputStream);
            final JkDependencySet deps = dependencies(uncomentedCode, baseDir, codeUrl);
            final List<Path>  projects = projects(uncomentedCode, baseDir, codeUrl);
            final List<String> compileOptions = compileOptions(uncomentedCode, codeUrl);
            return new SourceParser(deps, repos(uncomentedCode, codeUrl), projects, compileOptions);
        } catch (IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    private final JkDependencySet dependencies;

    private final JkRepoSet importRepos;

    private final List<Path> dependencyProjects;

    private final List<String> compileOptions;

    private SourceParser(JkDependencySet deps, JkRepoSet repos, List<Path>  dependencyProjects,
                         List<String> compileOptions) {
        super();
        this.dependencies = deps;
        this.importRepos = repos;
        this.dependencyProjects = Collections.unmodifiableList(dependencyProjects);
        this.compileOptions = compileOptions;
    }

    @SuppressWarnings("unchecked")
    private SourceParser and(SourceParser other) {
        return new SourceParser(this.dependencies.and(other.dependencies),
                this.importRepos.and(other.importRepos),
                JkUtilsIterable.concatLists(this.dependencyProjects, other.dependencyProjects),
                JkUtilsIterable.concatLists(this.compileOptions, other.compileOptions));
    }

    JkDependencySet dependencies() {
        return this.dependencies;
    }

    JkRepoSet importRepos() {
        return this.importRepos;
    }

    List<Path>  projects() {
        return this.dependencyProjects;
    }

    List<String> compileOptions() {
        return this.compileOptions;
    }

    private static JkDependencySet dependencies(String code, Path baseDir, URL url) {
        final List<String> deps = stringsInJkImport(code, url);
        return dependenciesFromImports(baseDir, deps);
    }

    private static JkRepoSet repos(String code, URL url) {
        final List<String> repoUrls = stringsInAnnotation(code, JkImportRepo.class, url);
        return JkRepoSet.of(repoUrls.toArray(new String[0]));
    }

    private static List<Path>  projects(String code, Path baseDir, URL url) {
        final List<String> deps = jkImportRun(code, url);
        return projectDependencies(baseDir, deps);
    }

    private static List<String>  compileOptions(String code, URL url) {
        return stringsInAnnotation(code, JkCompileOption.class, url);
    }

    private static JkDependencySet dependenciesFromImports(Path baseDir, List<String> deps) {
        JkDependencySet result = JkDependencySet.of();
        for (final String dependency : deps) {
            if (isModuleDependencyDescription(dependency)) {
                result = result.and(JkModuleDependency.of(dependency));
            } else  if (dependency.contains("*")) {
                if (dependency.contains("*")) {
                    for (Path path : JkPathTree.of(baseDir).andMatching(true, dependency).getFiles()) {
                        result = result.andFile(path);
                    }
                }
            } else {
                Path depFile = Paths.get(dependency);
                if (!Files.exists(depFile)) {
                    final Path relativeFile = baseDir.resolve(dependency);
                    if (Files.exists(relativeFile)) {
                        depFile = relativeFile.normalize();
                    } else {
                        JkLog.warn("File '" + dependency
                                + "' mentionned in @JkImport does not exist.");
                    }
                }
                result = result.andFile(depFile);
            }
        }
        return result;
    }


    /**
     * Returns <code>true</code> if the candidate string is a valid module dependency description.
     */
    private static boolean isModuleDependencyDescription(String candidate) {
        final int colonCount = JkUtilsString.countOccurence(candidate, ':');
        return colonCount == 2 || colonCount == 3;
    }

    private static List<Path>  projectDependencies(Path baseDir, List<String> deps) {
        final List<Path>  projects = new LinkedList<>();
        for (final String projectReltivePath : deps) {
            final Path file = baseDir.resolve(projectReltivePath);
            if (!Files.exists(file)) {
                throw new JkException("Folder " + file + " defined as project does not exists.");
            }
            projects.add(file);
        }
        return projects;
    }

    private static List<String> stringsInJkImport(String code, URL url) {
        return stringsInAnnotation(code, JkImport.class, url);
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringsInAnnotation(String code, Class<?> annotationClass, URL url) {
        final List<String> result = new LinkedList<>();
        try (final Scanner scanner = new Scanner(code)){
            scanner.useDelimiter("");

            while (scanner.hasNext()) {
                final String jkImportWord = scanner.findInLine("@" + annotationClass.getSimpleName());
                if (jkImportWord == null) {
                    final String nextLine = scanner.nextLine();
                    if (removeQuotes(nextLine).contains("class ")) {
                        return Collections.EMPTY_LIST;
                    }
                    continue;
                }
                final String context = " parsing @" + annotationClass.getSimpleName();
                final String between = extractStringTo(scanner, "(", url, context);
                if (!containsOnly(between, " ", "\n", "\r", "\t")) {
                    continue;
                }
                result.addAll(scanInsideAnnotation(scanner, url, context));
            }
        }
        return result;
    }


    private static boolean containsOnly(String stringToMatch, String... candidates) {
        String left = stringToMatch;
        for (final String candidate : candidates) {
            left = left.replace(candidate, "");
        }
        return left.isEmpty();
    }

    private static List<String> jkImportRun(String code, URL url) {
        return stringsInAnnotation(code, JkImportProject.class, url);
    }

    @SuppressWarnings("unchecked")
    private static List<String> scanInsideAnnotation(Scanner scanner, URL url, String context) {

        final String betweenParenthesis = extractStringTo(scanner, ")", url, context);
        final List<String> items = splitIgnoringQuotes(betweenParenthesis);
        for (final String item : items) {
            final String trimedItem = item.trim();
            if (trimedItem.startsWith("\"")) {
                return JkUtilsIterable.listOf(withoutQuotes(trimedItem));
            }
            if (trimedItem.startsWith("{")) {
                return curlyBraceToStrings(betweenParenthesis, url, context);
            }
            if (trimedItem.startsWith("value ") || trimedItem.startsWith("of=")) {
                final String after = JkUtilsString.substringAfterFirst(trimedItem, "=").trim();
                if (after.startsWith("\"")) {
                    return JkUtilsIterable.listOf(withoutQuotes(after));
                }
                if (after.startsWith("{")) {
                    return curlyBraceToStrings(after, url, context);
                }
            }
        }
        return Collections.EMPTY_LIST;
    }

    private static List<String> curlyBraceToStrings(String input, URL url, String context) {
        final Scanner innerScanner = new Scanner(input);
        innerScanner.useDelimiter("");
        final String braced = extractStringTo(innerScanner, "}", url, context);
        final List<String> elements = splitIgnoringQuotes(braced);
        final List<String> result = new LinkedList<>();
        for (final String element : elements) {
            result.add(withoutQuotes(element));
        }
        return result;
    }

    private static String withoutQuotes(String string) {
        return JkUtilsString.substringBeforeLast(JkUtilsString.substringAfterFirst(string, "\""),
                "\"");
    }

    private static List<String> splitIgnoringQuotes(String input) {
        final List<String> result = new LinkedList<>();
        int start = 0;
        boolean inQuotes = false;
        for (int current = 0; current < input.length(); current++) {
            if (input.charAt(current) == '\"') {
                inQuotes = !inQuotes; // toggle state
            }
            final boolean atLastChar = (current == input.length() - 1);
            if (atLastChar) {
                result.add(input.substring(start));
            } else if (input.charAt(current) == ',' && !inQuotes) {
                result.add(input.substring(start, current));
                start = current + 1;
            }
        }
        return result;
    }

    private static String extractStringTo(Scanner scanner, String delimiter, URL url, String context) {
        boolean inQuote = false;
        boolean escaping = false;
        final StringBuilder builder = new StringBuilder();
        final StringBuilder all = new StringBuilder();
        while (scanner.hasNext()) {
            final String character = scanner.next();
            all.append(character);
            if (!inQuote) {
                if (character.equals("\"")) {
                    inQuote = true;
                    builder.append(character);
                } else if (character.equals(delimiter)) {
                    return builder.toString();
                } else {
                    builder.append(character);
                }
            } else if (escaping) {
                escaping = false;
            } else { // in quote
                switch (character) {
                    case "\\":
                        escaping = true;
                        break;
                    case "\"":
                        inQuote = false;
                        builder.append(character);
                        break;
                    default:
                        builder.append(character);
                        break;
                }
            }
        }
        throw new IllegalStateException("No matching " + delimiter + " found" + context + " in "
                + url + ". " + all.toString());
    }

    private static String removeQuotes(String line) {
        boolean inQuote = false;
        boolean escaping = false;
        final StringBuilder builder = new StringBuilder();
        final Scanner scanner = new Scanner(line);
        while (scanner.hasNext()) {
            final String character = scanner.next();
            if (!inQuote) {
                if (character.equals("\"")) {
                    inQuote = true;
                } else {
                    builder.append(character);
                }
            } else if (escaping) {
                escaping = false;
            } else {
                if (character.equals("\\")) {
                    escaping = true;
                } else if (character.equals("\"")) {
                    inQuote = false;
                }
            }
        }
        scanner.close();
        return builder.toString();
    }

    private static String removeComments(InputStream inputStream) {
        final int outsideComment = 0;
        final int insideLineComment = 1;
        final int insideblockComment = 2;
        boolean insideStringLiteral = false;

        // we want to have at least one new line in the result if the block is
        // not inline.
        final int insideblockComment_noNewLineYet = 3;

        int currentState = outsideComment;
        final StringBuilder endResult = new StringBuilder();
        final Scanner scanner;
        scanner = new Scanner(inputStream);
        scanner.useDelimiter("");
        while (scanner.hasNext()) {
            final String character = scanner.next();
            switch (currentState) {
                case outsideComment:
                    if (insideStringLiteral) {
                        if (character.equals("\"")) {
                            insideStringLiteral = false;
                        }
                        endResult.append(character);
                    } else if (character.equals("/") && scanner.hasNext()) {
                        final String c2 = scanner.next();
                        switch (c2) {
                            case "/":
                                currentState = insideLineComment;
                                break;
                            case "*":
                                currentState = insideblockComment_noNewLineYet;
                                break;
                            default:
                                endResult.append(character).append(c2);
                                break;
                        }
                    } else if (character.equals("\"")) {
                        insideStringLiteral = true;
                        endResult.append(character);
                    } else {
                        endResult.append(character);
                    }
                    break;
                case insideLineComment:
                    if (character.equals("\n")) {
                        currentState = outsideComment;
                        endResult.append("\n");
                    }
                    break;
                case insideblockComment_noNewLineYet:
                    if (character.equals("\n")) {
                        endResult.append("\n");
                        currentState = insideblockComment;
                    }
                case insideblockComment:
                    while (character.equals("*") && scanner.hasNext()) {
                        final String c2 = scanner.next();
                        if (c2.equals("/")) {
                            currentState = outsideComment;
                            break;
                        }

                    }
            }
        }
        scanner.close();
        return endResult.toString();
    }

}
