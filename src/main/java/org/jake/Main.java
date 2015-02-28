package org.jake;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsString;

/**
 * Main class for launching Jake from command line.
 * 
 * @author Jerome Angibaud
 */
class Main {

	public static void main(String[] args) {
		displayIntro();
		JakeLog.info("Java Home : " + System.getProperty("java.home"));
		JakeLog.info("Java Version : " + System.getProperty("java.version")+ ", " + System.getProperty("java.vendor"));
		JakeLog.info("Jake class path : " + System.getProperty("java.class.path"));
		JakeLog.info("Command line : " + JakeUtilsString.join(Arrays.asList(args), " "));
		final Map<String, String> optionMap = new HashMap<String, String>();
		optionMap.putAll(loadOptionsProperties());
		final CommandLine commandLine = CommandLine.of(args);
		optionMap.putAll(commandLine.options());
		JakeOptions.init(optionMap);
		JakeLog.info("Using global options : " + JakeOptions.fieldOptionsToString(JakeOptions.instance()));
		JakeLog.info("And free form options : " + JakeOptions.freeFormToString());
		JakeLog.nextLine();
		defineSystemProps(args);

		final Project project = new Project(JakeUtilsFile.workingDir(), JakeUtilsFile.workingDir());

		final JakeClassLoader classLoader;
		if (project.hasBuildSource()) {
			final JakePath extraPath = project.resolveBuildPathAndCompile(BootstrapOptions.createPopulatedWithOptions());
			classLoader = JakeClassLoader.current().createChild(extraPath);
		} else {
			classLoader = JakeClassLoader.current();
		}
		System.out.println(classLoader);
		final boolean result = project.executeBuild(JakeUtilsFile.workingDir(), classLoader,
				commandLine.methods(), commandLine.pluginSetups());
		if (!result) {
			System.exit(1);  // NOSONAR
		}
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

	private static Map<String, String> loadOptionsProperties() {
		final File propFile = new File(JakeLocator.jakeHome(), "options.properties");
		if (propFile.exists()) {
			final Properties properties = JakeUtilsFile.readPropertyFile(propFile);
			return JakeUtilsIterable.propertiesToMap(properties);
		}
		return Collections.emptyMap();

	}

	private static int printAsciiArt1() {
		final InputStream inputStream = Main.class.getResourceAsStream("ascii1.txt");
		final List<String> lines = JakeUtilsIO.readLines(inputStream);
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
		final String version = JakeUtilsIO.readResourceIfExist("org/jake/version.txt");
		if (version != null) {
			JakeLog.info(JakeUtilsString.repeat(" ", lenght) + "Version : " + version);
		}
		JakeLog.nextLine();
	}

	private Main() {}

}
