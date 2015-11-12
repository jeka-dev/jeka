package org.jerkar.tool;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkModuleDependency;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

/*
 * Without doubt, the most crappy code of this project.
 * The important point is that we achieve parsing without using any dependencies : just the JDK.
 *
 * @author Jerome Angibaud
 */
final class JavaSourceParser {

    private static JavaSourceParser of(File baseDir, File code) {
	return of(baseDir, JkUtilsFile.toUrl(code));
    }

    public static JavaSourceParser of(File baseDir, Iterable<File> files) {
	JavaSourceParser result = new JavaSourceParser(JkDependencies.of(), JkRepos.of(), new LinkedList<File>());
	for (final File code : files) {
	    result = result.and(of(baseDir, code));
	}
	return result;
    }

    static JavaSourceParser of(File baseDir, URL codeUrl) {
	final InputStream inputStream = JkUtilsIO.inputStream(codeUrl);
	final String uncomentedCode = removeComments(inputStream);
	final JkDependencies deps = dependencies(uncomentedCode, baseDir, codeUrl);
	final List<File> projects = projects(uncomentedCode, baseDir, codeUrl);
	final JavaSourceParser result = new JavaSourceParser(deps, JkRepos.of(), projects);
	JkUtilsIO.closeQuietly(inputStream);
	return result;
    }

    private final JkDependencies dependencies;

    private final JkRepos importRepos;

    private final List<File> dependecyProjects;

    private JavaSourceParser(JkDependencies deps, JkRepos repos, List<File> dependencyProjects) {
	super();
	this.dependencies = deps;
	this.importRepos = repos;
	this.dependecyProjects = Collections.unmodifiableList(dependencyProjects);
    }

    @SuppressWarnings("unchecked")
    private JavaSourceParser and(JavaSourceParser other) {
	return new JavaSourceParser(this.dependencies.and(other.dependencies), this.importRepos.and(other.importRepos),
		JkUtilsIterable.concatLists(this.dependecyProjects, other.dependecyProjects));
    }

    public JkDependencies dependencies() {
	return this.dependencies;
    }

    public JkRepos importRepos() {
	return this.importRepos;
    }

    public List<File> projects() {
	return this.dependecyProjects;
    }

    private static JkDependencies dependencies(String code, File baseDir, URL url) {
	final List<String> deps = jkImports(code, url);
	return dependenciesFromImports(baseDir, deps);
    }

    private static List<File> projects(String code, File baseDir, URL url) {
	final List<String> deps = jkProjects(code, url);
	return projectDependencies(baseDir, deps);
    }

    private static JkDependencies dependenciesFromImports(File baseDir, List<String> deps) {
	final JkDependencies.Builder builder = JkDependencies.builder();
	for (final String dependency : deps) {
	    if (JkModuleDependency.isModuleDependencyDescription(dependency)) {
		builder.on(JkModuleDependency.of(dependency));
	    } else {
		if (dependency.contains(":")) {
		    throw new JkException("Dependency " + dependency
			    + " expressed in @JkImport is malformed, the expected format is groupId:artifactId:version.");
		}
		final File depFile = JkUtilsFile.canonicalFile(new File(baseDir, dependency));
		if (!depFile.exists()) {
		    throw new JkException("File " + depFile + " mentionned in @JkImport does not exist.");
		}
		builder.on(depFile);
	    }

	}
	return builder.build();
    }

    private static List<File> projectDependencies(File baseDir, List<String> deps) {
	final List<File> projects = new LinkedList<File>();
	for (final String projectReltivePath : deps) {
	    final File file = new File(baseDir, projectReltivePath);
	    if (!file.exists()) {
		throw new JkException(
			"Folder " + JkUtilsFile.canonicalPath(file) + " defined as project does not exists.");
	    }
	    projects.add(file);
	}
	return projects;
    }

    @SuppressWarnings("unchecked")
    private static List<String> jkImports(String code, URL url) {
	final Scanner scanner = new Scanner(code);
	scanner.useDelimiter("");
	while (scanner.hasNext()) {
	    final String jkImportWord = scanner.findInLine("@JkImport");
	    if (jkImportWord == null) {
		final String nextLine = scanner.nextLine();
		if (removeQuotes(nextLine).contains("class ")) {
		    return Collections.EMPTY_LIST;
		}
		continue;
	    }
	    final String context = " parsing @JkImport ";
	    final String between = extractStringTo(scanner, "(", url, context);
	    if (!containsOnly(between, " ", "\n", "\r", "\t")) {
		continue;
	    }

	    return scanInsideAnnotation(scanner, url, context);
	}
	return Collections.EMPTY_LIST;
    }

    private static boolean containsOnly(String stringToMatch, String... candidates) {
	String left = stringToMatch;
	for (final String candidate : candidates) {
	    left = left.replace(candidate, "");
	}
	return left.isEmpty();
    }

    private static List<String> jkProjects(String code, URL url) {
	final Scanner scanner = new Scanner(code);
	scanner.useDelimiter("");
	final List<String> result = new LinkedList<String>();
	while (scanner.hasNext()) {
	    final String jkImportWord = scanner.findInLine("@JkProject");
	    if (jkImportWord == null) {
		scanner.nextLine();
		continue;
	    }
	    final String context = " parsing @JkProject ";
	    final String between = extractStringTo(scanner, "(", url, context);
	    if (!containsOnly(between, " ", "\n", "\r", "\t")) {
		continue;
	    }
	    result.addAll(scanInsideAnnotation(scanner, url, context));
	}
	return result;
    }

    @SuppressWarnings("unchecked")
    private static List<String> scanInsideAnnotation(Scanner scanner, URL url, String context) {

	final String betweenParenthesis = extractStringTo(scanner, ")", url, context);
	final List<String> items = splitIgnoringQuotes(betweenParenthesis, ',');
	for (final String item : items) {
	    final String trimedItem = item.trim();
	    if (trimedItem.startsWith("\"")) {
		return JkUtilsIterable.listOf(withoutQuotes(trimedItem));
	    }
	    if (trimedItem.startsWith("{")) {
		return curlyBraceToStrings(betweenParenthesis, url, context);
	    }
	    if (trimedItem.startsWith("value ") || trimedItem.startsWith("value=")) {
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
	final List<String> elements = splitIgnoringQuotes(braced, ',');
	final List<String> result = new LinkedList<String>();
	for (final String element : elements) {
	    result.add(withoutQuotes(element));
	}
	return result;
    }

    private static String withoutQuotes(String string) {
	return JkUtilsString.substringBeforeLast(JkUtilsString.substringAfterFirst(string, "\""), "\"");
    }

    private static List<String> splitIgnoringQuotes(String input, char delimiter) {
	final List<String> result = new LinkedList<String>();
	int start = 0;
	boolean inQuotes = false;
	for (int current = 0; current < input.length(); current++) {
	    if (input.charAt(current) == '\"') {
		inQuotes = !inQuotes; // toggle state
	    }
	    final boolean atLastChar = (current == input.length() - 1);
	    if (atLastChar) {
		result.add(input.substring(start));
	    } else if (input.charAt(current) == delimiter && !inQuotes) {
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
		if (character.equals("\\")) {
		    escaping = true;
		} else if (character.equals("\"")) {
		    inQuote = false;
		    builder.append(character);
		} else {
		    builder.append(character);
		}
	    }
	}
	throw new IllegalStateException(
		"No matching " + delimiter + " found" + context + "in " + url + ". " + all.toString());
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
		if (character.equals("/") && scanner.hasNext()) {
		    final String c2 = scanner.next();
		    if (c2.equals("/")) {
			currentState = insideLineComment;
		    } else if (c2.equals("*")) {
			currentState = insideblockComment_noNewLineYet;
		    } else {
			endResult.append(character).append(c2);
		    }
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
