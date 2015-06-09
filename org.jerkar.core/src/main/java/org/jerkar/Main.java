package org.jerkar;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsIO;
import org.jerkar.utils.JkUtilsString;
import org.jerkar.utils.JkUtilsTime;

/**
 * Main class for launching Jerkar from command line.
 * 
 * @author Jerome Angibaud
 */
class Main {

	public static void main(String[] args) {
		final long start = System.nanoTime();
		displayIntro();
		JkLog.info("Java Home : " + System.getProperty("java.home"));
		JkLog.info("Java Version : " + System.getProperty("java.version")+ ", " + System.getProperty("java.vendor"));
		JkLog.info("Jerkar User home : " + JkLocator.jerkarUserHome().getAbsolutePath());
		JkLog.info("Jerkar class path : " + System.getProperty("java.class.path"));
		JkLog.info("Command line : " + JkUtilsString.join(Arrays.asList(args), " "));
		final Map<String, String> optionMap = new HashMap<String, String>();
		optionMap.putAll(loadOptionsProperties());
		final CommandLine commandLine = CommandLine.of(args);
		optionMap.putAll(commandLine.getSubProjectBuildOptions());
		optionMap.putAll(commandLine.getMasterBuildOptions() );
		JkOptions.init(optionMap);
		JkLog.info("Using global options : " + JkOptions.fieldOptionsToString(JkOptions.instance()));
		JkLog.info("And free form options : " + JkOptions.freeFormToString());
		defineSystemProps(args);
		final File workingDir = JkUtilsFile.workingDir();
		final Project project = new Project(workingDir);
		JkLog.nextLine();
		try {
			project.execute(commandLine, JkOptions.buildClass());
			final int lenght = printAscii(false, "succes.ascii");
			System.out.println(JkUtilsString.repeat(" ", lenght) + "Total build time : "
					+ JkUtilsTime.durationInSeconds(start) + " seconds.");
		} catch (final RuntimeException e) {
			System.err.println();
			e.printStackTrace(System.err);
			final int lenght = printAscii(true, "failed.ascii");
			System.err.println(JkUtilsString.repeat(" ", lenght) + "Total build time : "
					+ JkUtilsTime.durationInSeconds(start) + " seconds.");
			System.exit(1);
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
		final File propFile = new File(JkLocator.jerkarHome(), "system.properties");
		final Map<String, String> result = new HashMap<String, String>();
		if (propFile.exists()) {
			result.putAll(JkUtilsFile.readPropertyFileAsMap(propFile));
		}
		final File userPropFile = new File(JkLocator.jerkarUserHome(), "system.properties");
		if (userPropFile.exists()) {
			result.putAll(JkUtilsFile.readPropertyFileAsMap(userPropFile));
		}
		for (final Map.Entry<String, String> entry : result.entrySet()) {
			System.setProperty(entry.getKey(), entry.getValue());
		}

	}

	private static Map<String, String> loadOptionsProperties() {
		final File propFile = new File(JkLocator.jerkarHome(), "options.properties");
		final Map<String, String> result = new HashMap<String, String>();
		if (propFile.exists()) {
			result.putAll(JkUtilsFile.readPropertyFileAsMap(propFile));
		}
		final File userPropFile = new File(JkLocator.jerkarUserHome(), "options.properties");
		if (userPropFile.exists()) {
			result.putAll(JkUtilsFile.readPropertyFileAsMap(userPropFile));
		}
		return result;
	}

	private static int printAscii(boolean error, String fileName) {
		final InputStream inputStream = Main.class.getResourceAsStream(fileName);
		final List<String> lines = JkUtilsIO.readAsLines(inputStream);
		int i = 0;
		for (final String line: lines) {
			if (i < line.length()) {
				i = line.length();
			}
			if (error) {
				System.err.println(line);
			} else {
				System.out.println(line);
			}
		}
		return i;
	}

	private static void displayIntro() {
		final int lenght = printAscii(false, "jerkar.ascii");
		JkLog.info(JkUtilsString.repeat(" ", lenght) + "The 100% Java build tool.");
		final String version = JkUtilsIO.readResourceIfExist("org/jerkar/version.txt");
		if (version != null) {
			JkLog.info(JkUtilsString.repeat(" ", lenght) + "Version : " + version);
		}
		JkLog.nextLine();
	}

	private Main() {}

}
