package org.jake;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jake.file.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsString;

public class JakeLauncher {

	private static final String DEFAULT_METHOD = "base";

	public static void main(String[] args) {
		displayIntro();
		JakeLog.info("Java Home : " + System.getProperty("java.home"));
		JakeLog.info("Java Version : " + System.getProperty("java.version")+ ", " + System.getProperty("java.vendor"));
		JakeLog.info("Jake class path : " + System.getProperty("java.class.path"));
		OptionStore.options = extractOptions(args);
		JakeLog.info("Using global options : " + JakeOptions.fieldOptionsToString(JakeOptions.INSTANCE));
		JakeLog.info("And free form options : " + JakeOptions.freeFormToString());
		JakeLog.nextLine();
		defineSystemProps(args);
		final List<String> actions = extractActions(args);
		final ProjectBuilder projectBuilder = new ProjectBuilder(JakeUtilsFile.workingDir());
		final boolean result = projectBuilder.build(actions);
		if (!result) {
			System.exit(1);
		}
	}

	private static List<String> extractActions(String[] args) {
		final List<String> result = new LinkedList<String>();
		for (final String arg : args) {
			if (!arg.startsWith("-")) {
				result.add(arg);
			}
		}
		if (result.isEmpty()) {
			result.add(DEFAULT_METHOD);
		}
		return result;
	}

	private static void defineSystemProps(String[] args) {
		for (final String arg : args) {
			if (arg.startsWith("-D")) {
				final int equalIndex = arg.indexOf("=");
				if (equalIndex <= -1) {
					System.setProperty(arg.substring(2), "");
				} else {
					final String name = arg.substring(2, equalIndex);
					final String value = arg.substring(equalIndex+1);
					System.setProperty(name, value);
				}
			}
		}
	}

	private static Map<String, String> extractOptions(String[] args) {
		final Map<String, String> result = new HashMap<String, String>();
		for (final String arg : args) {
			if (arg.startsWith("-") && !arg.startsWith("-D")) {
				final int equalIndex = arg.indexOf("=");
				if (equalIndex <= -1) {
					result.put(arg.substring(1), null);
				} else {
					final String name = arg.substring(1, equalIndex);
					final String value = arg.substring(equalIndex+1);
					result.put(name, value);
				}
			}
		}
		return result;
	}


	public static int printAsciiArt1() {
		final InputStream inputStream = JakeLauncher.class.getResourceAsStream("ascii1.txt");
		final List<String> lines = JakeUtilsFile.toLines(inputStream);
		int i = 0;
		for (final String line: lines) {
			if (i < line.length()) {
				i = line.length();
			}
			JakeLog.info(line);
		}
		return i;
	}

	private static void displayIntro() {
		final int lenght = printAsciiArt1();
		JakeLog.info(JakeUtilsString.repeat(" ", lenght) + "The 100% Java build system.");
		final String version = JakeUtilsFile.readResourceIfExist("org/jake/version.txt");
		if (version != null) {
			JakeLog.info(JakeUtilsString.repeat(" ", lenght) + "Version : " + version);
		}
		JakeLog.nextLine();
	}

}
