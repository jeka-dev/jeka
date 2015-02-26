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

	public static JavaSourceParser of(File baseDir, File code) {
		final InputStream inputStream = JakeUtilsIO.inputStream(code);
		final JavaSourceParser result = new JavaSourceParser(dependencies(inputStream, baseDir, JakeUtilsFile.toUrl(code)));
		JakeUtilsIO.closeQuietly(inputStream);
		return result;
	}

	public static JavaSourceParser of(File baseDir, Iterable<File> files) {
		final JakeDependencies.Builder builder = JakeDependencies.builder();
		for (final File code : files) {
			final InputStream inputStream = JakeUtilsIO.inputStream(code);
			builder.on(dependencies(inputStream, baseDir, JakeUtilsFile.toUrl(code)));
			JakeUtilsIO.closeQuietly(inputStream);
		}
		return new JavaSourceParser(builder.build());
	}

	public static JavaSourceParser of(File baseDir, URL code) {
		final InputStream inputStream = JakeUtilsIO.inputStream(code);
		final JavaSourceParser result = new JavaSourceParser(dependencies(inputStream, baseDir, code));
		JakeUtilsIO.closeQuietly(inputStream);
		return result;
	}

	private final JakeDependencies dependencies;

	private JavaSourceParser(JakeDependencies deps) {
		super();
		this.dependencies = deps;
	}

	public JakeDependencies dependencies() {
		return this.dependencies;
	}

	private static JakeDependencies dependencies(InputStream code, File baseDir, URL url) {
		final String uncomentedCode = removeComments(code);
		final List<String> deps = jakeImports(uncomentedCode, url);
		return dependencies(baseDir, deps);
	}

	private static JakeDependencies dependencies(File baseDir, List<String> deps) {
		final JakeDependencies.Builder builder = JakeDependencies.builder().usingDefaultScopes(Project.JAKE_SCOPE);
		for (final String dependency : deps) {
			if (JakeDependency.isGroupNameAndVersion(dependency)) {
				builder.on(JakeDependency.of(dependency));
			} else {
				builder.on(JakeDependency.ofFile(baseDir, dependency));
			}

		}
		return builder.build();
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
			final String between  = extractStringTo(scanner, "(", url, " parsing @JakeImport ");
			if(!JakeUtilsString.containsOnly(between, " ", "\n", "\r", "\t")) {
				continue;
			}
			return scanJakeImport(scanner, url);
		}
		return Collections.EMPTY_LIST;
	}

	@SuppressWarnings("unchecked")
	private static List<String> scanJakeImport(Scanner scanner, URL url) {
		final String context = " parsing @JakeImport ";
		final String arg = extractStringTo(scanner, ")", url, context);
		final List<String> items = splitIgnoringQuotes(arg, ',');
		for (final String item : items) {
			final String trimedItem = item.trim();
			if (JakeUtilsString.startsWithAny(trimedItem, "\"", "{")) {
				return startWithQuoteOrCurly(trimedItem, url, context);
			}
			if (trimedItem.startsWith("value ") || trimedItem.startsWith("value=")) {
				final String after = JakeUtilsString.substringAfterFirst(trimedItem, "=").trim();
				return startWithQuoteOrCurly(after, url, context);
			}
		}
		return Collections.EMPTY_LIST;
	}

	@SuppressWarnings("unchecked")
	private static List<String> startWithQuoteOrCurly(String input, URL url, String context) {
		if (input.startsWith("\"")) {
			return JakeUtilsIterable.listOf(withoutQuotes(input));
		}
		if (input.startsWith("{")) {
			return braceToStrings(input, url, context);
		}
		return Collections.EMPTY_LIST;
	}

	private static List<String> braceToStrings(String input, URL url, String context) {
		final Scanner innerScanner = new Scanner(input);
		innerScanner.findInLine("\\{");
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
		while (scanner.hasNext()) {
			final String character = scanner.next();
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
			} else {
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
		throw new IllegalStateException("No matching ) found" + context + " in " +  url + "");
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
