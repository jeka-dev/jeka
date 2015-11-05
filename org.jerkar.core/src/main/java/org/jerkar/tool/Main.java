package org.jerkar.tool;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsObject;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsTime;

/**
 * Main class for launching Jerkar from command line.
 * 
 * @author Jerome Angibaud
 */
final class Main {

	public static void main(String[] args) {
		final long start = System.nanoTime();
		displayIntro();
		final LoadResult loadResult = loadOptionsAndSystemProps(args);
		JkLog.info("Working Directory : " + System.getProperty("user.dir"));
		JkLog.info("Java Home : " + System.getProperty("java.home"));
		JkLog.info("Java Version : " + System.getProperty("java.version")+ ", " + System.getProperty("java.vendor"));
		JkLog.info("Jerkar Home : " + JkLocator.jerkarHome().getAbsolutePath());
		JkLog.info("Jerkar User Home : " + JkLocator.jerkarUserHome().getAbsolutePath());
		JkLog.info("Jerkar Repository Cache : " + JkLocator.jerkarRepositoryCache());
		JkLog.info("Jerkar Classpath : " + System.getProperty("java.class.path"));
		JkLog.info("Command Line : " + JkUtilsString.join(Arrays.asList(args), " "));
		logProps("Specified System Properties", loadResult.sysprops);
		JkLog.info("Standard Options : " + loadResult.standardOptions);
		logProps("Options", JkOptions.toDisplayedMap(JkOptions.getAll()));

		final File workingDir = JkUtilsFile.workingDir();
		final Project project = new Project(workingDir);
		JkLog.nextLine();
		try {
			project.execute(loadResult.commandLine, loadResult.standardOptions.buildClass);
			final int lenght = printAscii(false, "success.ascii");
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

	private static LoadResult loadOptionsAndSystemProps(String[] args) {
		final Map<String, String> sysProps = getSpecifiedSystemProps(args);
		JkUtilsTool.setSystemProperties(sysProps);
		final Map<String, String> optionMap = new HashMap<String, String>();
		optionMap.putAll(loadOptionsProperties());
		final CommandLine commandLine = CommandLine.of(args);
		optionMap.putAll(commandLine.getSubProjectBuildOptions());
		optionMap.putAll(commandLine.getMasterBuildOptions() );
		JkOptions.init(optionMap);
		final StandardOptions standardOptions = new StandardOptions();
		JkLog.silent(standardOptions.silent);
		JkLog.verbose(standardOptions.verbose);

		JkOptions.populateFields(standardOptions);
		final LoadResult loadResult = new LoadResult();
		loadResult.sysprops = sysProps;
		loadResult.commandLine = commandLine;
		loadResult.standardOptions = standardOptions;
		return loadResult;
	}

	static void logProps(String message, Map<String, String> props) {
		if (props.isEmpty()) {
			JkLog.info(message + " : none.");
		} else if (props.size() <= 3) {
			JkLog.info(message + " : " + JkUtilsIterable.toString(props));
		} else {
			JkLog.info(message + " : ");
			JkLog.delta(1);
			JkLog.info(JkUtilsIterable.toStrings(props));
			JkLog.delta(-1);
		}
	}



	private static Map<String, String> getSpecifiedSystemProps(String[] args) {
		final Map<String, String> result = new TreeMap<String, String>();
		final File propFile = new File(JkLocator.jerkarHome(), "system.properties");
		if (propFile.exists()) {
			result.putAll(JkUtilsFile.readPropertyFileAsMap(propFile));
		}
		result.putAll(JkUtilsTool.userSystemProperties());
		for (final String arg : args) {
			if (arg.startsWith("-D")) {
				final int equalIndex = arg.indexOf("=");
				if (equalIndex <= -1) {
					result.put(arg.substring(2), "");
				} else {
					final String name = arg.substring(2, equalIndex);
					final String value = arg.substring(equalIndex+1);
					result.put(name, value);
				}
			}
		}
		return result;
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

	private static class StandardOptions {

		boolean verbose;

		boolean silent;

		String buildClass;

		@Override
		public String toString() {
			return "buildClass=" + JkUtilsObject.toString(buildClass) + ", verbose=" + verbose + ", silent=" + silent;
		}

	}

	private static class LoadResult {

		private Map<String, String> sysprops;

		private CommandLine commandLine;

		private StandardOptions standardOptions;
	}

}
