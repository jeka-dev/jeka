package org.jake;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeDependency;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeScopeMapping;
import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsString;

/*
 * Without doubt, the most crappy code of this project.
 * The important point is that we achieve parsing without using any dependencies : just the JDK.
 * 
 * @author Jerome Angibaud
 */
class JavaSourceParser {

	private static final JakeScopeMapping SCOPE_MAPPING = JakeScopeMapping.of(Project.JAKE_SCOPE).to("default(*)");


	public static JavaSourceParser of(File baseDir, File code) {
		return of(baseDir, JakeUtilsFile.toUrl(code));
	}

	public static JavaSourceParser of(File baseDir, Iterable<File> files) {
		JavaSourceParser result = new JavaSourceParser(JakeDependencies.on(), JakeRepos.of(), new LinkedList<File>());
		for (final File code : files) {
			result = result.and(of(baseDir, code));
		}
		return result;
	}

	public static JavaSourceParser of(File baseDir, URL codeUrl) {
		final InputStream inputStream = JakeUtilsIO.inputStream(codeUrl);
		final String uncomentedCode = removeComments(inputStream);
		final JakeDependencies deps = dependencies(uncomentedCode, baseDir, codeUrl);
		final List<File> projects = projects(uncomentedCode, baseDir, codeUrl);
		final JavaSourceParser result = new JavaSourceParser(deps, JakeRepos.of(), projects);
		JakeUtilsIO.closeQuietly(inputStream);
		return result;
	}

	private final JakeDependencies dependencies;

	private final JakeRepos importRepos;

	private final List<File> dependecyProjects;

	private JavaSourceParser(JakeDependencies deps, JakeRepos repos, List<File> dependencyProjects) {
		super();
		this.dependencies = deps;
		this.importRepos = repos;
		this.dependecyProjects = Collections.unmodifiableList(dependencyProjects);
	}

	@SuppressWarnings("unchecked")
	private JavaSourceParser and(JavaSourceParser other) {
		return new JavaSourceParser(this.dependencies.and(other.dependencies),
				this.importRepos.and(other.importRepos),
				JakeUtilsIterable.concatLists(this.dependecyProjects, other.dependecyProjects) );
	}

	public JakeDependencies dependencies() {
		return this.dependencies;
	}

	public JakeRepos importRepos() {
		return this.importRepos;
	}

	public List<File> projects() {
		return this.dependecyProjects;
	}

	private static JakeDependencies dependencies(String code, File baseDir, URL url) {
		final List<String> deps = jakeImports(code, url);
		return dependenciesFromImports(baseDir, deps);
	}

	private static List<File> projects(String code, File baseDir, URL url) {
		final List<String> deps = jakeProjects(code, url);
		return projectDependencies(baseDir, deps);
	}


	private static JakeDependencies dependenciesFromImports(File baseDir, List<String> deps) {
		final JakeDependencies.Builder builder = JakeDependencies.builder().usingDefaultScopeMapping(SCOPE_MAPPING);
		for (final String dependency : deps) {
			if (JakeDependency.isGroupNameAndVersion(dependency)) {
				builder.on(JakeDependency.of(dependency));
			} else {
				builder.on(JakeDependency.ofFile(baseDir, dependency));
			}

		}
		return builder.build();
	}

	private static List<File> projectDependencies(File baseDir, List<String> deps) {
		final List<File> projects = new LinkedList<File>();
		for (final String projectReltivePath : deps) {
			projects.add(new File(baseDir, projectReltivePath));
		}
		return projects;
	}

	@SuppressWarnings("unchecked")
	private static List<String> jakeImports(String code, URL url) {
		final Scanner scanner = new Scanner(code);
		scanner.useDelimiter("");
		while (scanner.hasNext()) {
			final String jakeImportWord = scanner.findInLine("@JakeImport");
			if (jakeImportWord == null) {
				final String nextLine = scanner.nextLine();
				if (removeQuotes(nextLine).contains("class ")) {
					return Collections.EMPTY_LIST;
				}
				continue;
			}
			final String context = " parsing @JakeImport ";
			final String between  = extractStringTo(scanner, "(", url, context);
			if(!JakeUtilsString.containsOnly(between, " ", "\n", "\r", "\t")) {
				continue;
			}

			return scanInsideAnnotation(scanner, url, context);
		}
		return Collections.EMPTY_LIST;
	}


	private static List<String> jakeProjects(String code, URL url) {
		final Scanner scanner = new Scanner(code);
		scanner.useDelimiter("");
		final List<String> result = new LinkedList<String>();
		while (scanner.hasNext()) {
			final String jakeImportWord = scanner.findInLine("@JakeProject");
			if (jakeImportWord == null) {
				scanner.nextLine();
				continue;
			}
			final String context = " parsing @JakeProject ";
			final String between  = extractStringTo(scanner, "(", url, context);
			if(!JakeUtilsString.containsOnly(between, " ", "\n", "\r", "\t")) {
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
				return JakeUtilsIterable.listOf(withoutQuotes(trimedItem));
			}
			if (trimedItem.startsWith("{")) {
				return curlyBraceToStrings(betweenParenthesis, url, context);
			}
			if (trimedItem.startsWith("value ") || trimedItem.startsWith("value=")) {
				final String after = JakeUtilsString.substringAfterFirst(trimedItem, "=").trim();
				if (after.startsWith("\"")) {
					return JakeUtilsIterable.listOf(withoutQuotes(after));
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
		return JakeUtilsString.substringBeforeLast(JakeUtilsString.substringAfterFirst(string, "\""), "\"");
	}

	private static List<String> splitIgnoringQuotes(String input, char delimiter) {
		final List<String> result = new LinkedList<String>();
		int start = 0;
		boolean inQuotes = false;
		for (int current = 0; current < input.length(); current++) {
			if (input.charAt(current) == '\"')
			{
				inQuotes = !inQuotes; // toggle state
			}
			final boolean atLastChar = (current == input.length() - 1);
			if(atLastChar) {
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
			} else if (escaping){
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
		throw new IllegalStateException("No matching " + delimiter + " found" + context + "in " +  url + ". " + all.toString());
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
			} else if (escaping){
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

		// we want to have at least one new line in the result if the block is not inline.
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
