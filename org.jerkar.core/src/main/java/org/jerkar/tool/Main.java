package org.jerkar.tool;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jerkar.api.system.JkEvent;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsTime;

/**
 * Main class for launching Jerkar from command line.
 *
 * @author Jerome Angibaud
 */
public final class Main {

    /**
     * Entry point for Jerkar application when launched from command-line
     */
    public static void main(String[] args) {
        final long start = System.nanoTime();
        final JkInit init = JkInit.of(args);
        JkEvent.register(new LogHandler());
        if (JkEvent.verbosity() != JkEvent.Verbosity.MUTE) {
            displayIntro();
        }
        init.displayInfo();

        final Path workingDir = Paths.get("").toAbsolutePath();
        final Engine engine = new Engine(workingDir);
        try {
            engine.execute(init);
            if (JkEvent.verbosity() != JkEvent.Verbosity.MUTE) {
                final int length = printAscii(false, "success.ascii");
                System.out.println(JkUtilsString.repeat(" ", length) + "Total build time : "
                        + JkUtilsTime.durationInSeconds(start) + " seconds.");
            }
        } catch (final RuntimeException e) {
            System.err.println();
            e.printStackTrace(System.err);
            final int lenght = printAscii(true, "failed.ascii");
            System.err.println(JkUtilsString.repeat(" ", lenght) + "Total build time : "
                    + JkUtilsTime.durationInSeconds(start) + " seconds.");
            System.exit(1);
        }
    }

    /**
     * Entry point to call Jerkar on a given folder
     */
    public static void exec(Path projectDir, String... args) {
        final JkInit init = JkInit.of(args);
        final Engine engine = new Engine(projectDir);
        engine.execute(init);
    }

    private static int printAscii(boolean error, String fileName) {
        final InputStream inputStream = Main.class.getResourceAsStream(fileName);
        final List<String> lines = JkUtilsIO.readAsLines(inputStream);
        int i = 0;
        for (final String line : lines) {
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
        JkEvent.info(Main.class,JkUtilsString.repeat(" ", lenght) + "The 100% Java build tool.\n");
    }

    private Main() {
    }

}
