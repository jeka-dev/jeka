package org.jake;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsString;

class JavaSourceParser {

	private final String uncommantedCode;

	public boolean inQuote;

	private JavaSourceParser(String uncommantedCode) {
		super();
		this.uncommantedCode = uncommantedCode;
	}

	public List<String> imports() {
		for (final String fullLine : lines) {
			final String line = fullLine.toString();
		}
	}

	private String removeLeadingSpacesAndTabs(String line) {

	}

	@SuppressWarnings("unchecked")
	private static List<String> jakeImports(String code) {
		final Scanner scanner = new Scanner(code);
		scanner.useDelimiter("");
		while (scanner.hasNext()) {
			final String jakeImportWord = scanner.findInLine("@JakeImport");
			if (jakeImportWord == null) {
				final String nextLine = scanner.nextLine();
				if (removeQuotes(nextLine).contains("class ")) {
					return Collections.EMPTY_LIST;
				}
			}
			String chararcter = scanner.next();
			if (chararcter.equals("(")) {
				return scanJakeImport(scanner);
			}
			if (!chararcter.equals("\n") || !chararcter.equals(" ") || chararcter.equals("(")) {
				while (scanner.hasNext()) {
					chararcter = scanner.next();
					if (chararcter.equals("(")) {
						return scanJakeImport(scanner);
					}
				}
			}
		}
		return Collections.EMPTY_LIST;
	}

	@SuppressWarnings("unchecked")
	private static List<String> scanJakeImport(Scanner scanner) {
		final String arg = extractStringTo(scanner, ")");
		final List<String> items = splitIgnoringQuotes(arg, ',');
		for (final String item : items) {
			final String trimedItem = item.trim();
			if (JakeUtilsString.startsWithAny(trimedItem, "\"", "{")) {
				return startWithQuoteOrCurly(trimedItem);
			}
			if (trimedItem.startsWith("value ") || trimedItem.startsWith("value=")) {
				final String after = JakeUtilsString.substringAfterFirst(trimedItem, "=").trim();
				return startWithQuoteOrCurly(after);
			}
		}
		return Collections.EMPTY_LIST;
	}

	@SuppressWarnings("unchecked")
	private static List<String> startWithQuoteOrCurly(String input) {
		if (input.startsWith("\"")) {
			return JakeUtilsIterable.listOf(withoutQuotes(input));
		}
		if (input.startsWith("{")) {
			return braceToStrings(input);
		}
		return Collections.EMPTY_LIST;
	}

	private static List<String> braceToStrings(String input) {
		final Scanner innerScanner = new Scanner(input);
		final String braced = extractStringTo(innerScanner, "}");
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

	private static String extractStringTo(Scanner scanner, String delimiter) {
		boolean inQuote = false;
		boolean escaping = false;
		final StringBuilder builder = new StringBuilder();
		while (scanner.hasNext()) {
			final String character = scanner.next();
			if (!inQuote) {
				if (character.equals("\"")) {
					inQuote = true;
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
				}
			}
		}
		throw new IllegalStateException("No ) found.");
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

	private static String removeComments(File file) {
		final int outsideComment = 0;
		final int insideLineComment = 1;
		final int insideblockComment = 2;

		// we want to have at least one new line in the result if the block is not inline.
		final int insideblockComment_noNewLineYet = 3;

		int currentState = outsideComment;
		final StringBuilder endResult = new StringBuilder();
		final Scanner scanner;
		try {
			scanner = new Scanner(file);
		} catch (final FileNotFoundException e) {
			throw new RuntimeException(e);
		}
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
