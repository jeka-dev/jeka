package org.jake;

import java.util.LinkedList;
import java.util.List;

import org.jake.file.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsString;

public class JakeLauncher {

	public static void main(String[] args) {
		printAscciArt();
		JakeLog.info(JakeUtilsString.repeat(" ", 70) + "The 100% Java build system.");
		JakeLog.nextLine();
		defineSystemProps(args);
		final List<String> actions = extractAcions(args);
		final ProjectBuilder projectBuilder = new ProjectBuilder(JakeUtilsFile.workingDir());
		final boolean result = projectBuilder.build(actions);
		if (!result) {
			System.exit(1);
		}
	}

	private static List<String> extractAcions(String[] args) {
		final List<String> result = new LinkedList<String>();
		for (final String arg : args) {
			if (!arg.startsWith("-")) {
				result.add(arg);
			}
		}
		return result;
	}

	private static void defineSystemProps(String[] args) {
		for (final String arg : args) {
			if (arg.startsWith("-D")) {
				final int equalIndex = arg.indexOf("=");
				if (equalIndex <= -1) {
					System.out.println("++++++++++ add sys prop " + arg.substring(2) + "=''");
					System.setProperty(arg.substring(2), "");
				} else {
					final String name = arg.substring(2, equalIndex);
					final String value = arg.substring(equalIndex+1);
					System.out.println("++++++++++ add sys prop " + name + "=" + value );
					System.setProperty(name, value);
				}
			}
		}
	}



	private static void printAscciArt() {
		JakeLog.info("             _              _                   _                 _      ");
		JakeLog.info("            /\\ \\           / /\\                /\\_\\              /\\ \\    ");
		JakeLog.info("            \\ \\ \\         / /  \\              / / /  _          /  \\ \\   ");
		JakeLog.info("            /\\ \\_\\       / / /\\ \\            / / /  /\\_\\       / /\\ \\ \\  ");
		JakeLog.info("           / /\\/_/      / / /\\ \\ \\          / / /__/ / /      / / /\\ \\_\\ ");
		JakeLog.info("  _       / / /        / / /  \\ \\ \\        / /\\_____/ /      / /_/_ \\/_/ ");
		JakeLog.info(" /\\ \\    / / /        / / /___/ /\\ \\      / /\\_______/      / /____/\\    ");
		JakeLog.info(" \\ \\_\\  / / /        / / /_____/ /\\ \\    / / /\\ \\ \\        / /\\____\\/    ");
		JakeLog.info(" / / /_/ / /        / /_________/\\ \\ \\  / / /  \\ \\ \\      / / /______    ");
		JakeLog.info("/ / /__\\/ /        / / /_       __\\ \\_\\/ / /    \\ \\ \\    / / /_______\\   ");
		JakeLog.info("\\/_______/         \\_\\___\\     /____/_/\\/_/      \\_\\_\\   \\/__________/   ");
	}

}
