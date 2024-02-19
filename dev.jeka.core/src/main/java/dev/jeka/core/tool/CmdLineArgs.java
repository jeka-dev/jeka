package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import static dev.jeka.core.tool.JkConstants.*;

class CmdLineArgs {

    private final String[] args;

    CmdLineArgs(String ...args) {
        this.args = args;
    }

    private CmdLineArgs(List<String> args) {
        this(args.toArray(new String[0]));
    }

    CmdLineArgs withoutShellArgs() {
        return new CmdLineArgs(filterShellArgs(args));
    }

    CmdLineArgs interpolated(JkProperties properties) {
        return new CmdLineArgs(interpolatedCommandLine(args, properties));
    }

    CmdLineArgs and(CmdLineArgs other) {
        List<String> allArgs = Arrays.asList(args);
        allArgs.addAll(Arrays.asList(other.args));
        return new CmdLineArgs(allArgs.toArray(new String[0]));
    }

    CmdLineArgs withOptionsOnly() {
        String[] options = Arrays.stream(args)
                .filter(arg -> arg.startsWith("-"))
                .toArray(String[]::new);
        return new CmdLineArgs(options);
    }

    CmdLineArgs withoutOptions() {
        String[] options = Arrays.stream(args)
                .filter(arg -> !arg.startsWith("-"))
                .toArray(String[]::new);
        return new CmdLineArgs(options);
    }

    boolean isUsageHelpRequested() {
        return args.length == 0 || args[0].equals("--help") || args[0].equals("-h");
    }

    boolean isVersionHelpRequested() {
        return args.length > 0 && (args[0].equals("--version") || args[0].equals("-V"));
    }

    List<CmdLineArgs> splitByKbeanContext() {
        List<CmdLineArgs> result = new LinkedList<>();
        List<String> currentArgs = new LinkedList<>();
        for (String arg : args) {
            if (isKbeanRef(arg)) {
                if (!currentArgs.isEmpty()) {
                    CmdLineArgs subArgs = new CmdLineArgs(currentArgs);
                    result.add(subArgs);
                    currentArgs.clear();
                }
            }
            currentArgs.add(arg);
        }
        result.add(new CmdLineArgs(currentArgs));
        return result;
    }

    static boolean isKbeanRef(String arg) {
        return arg.endsWith(":");
    }

    private static String[] filterShellArgs(String[] originalArgs) {
        if (originalArgs.length == 0) {
            return originalArgs;
        }
        List<String> result = new LinkedList<>(Arrays.asList(originalArgs));
        String first =result.get(0);
        if (JkUtilsIterable.listOf("-r", "-rc").contains(first)) {
            result.remove(0);
            result.remove(0);
        } else if (first.startsWith("@")) {   // remove remote @alias
            result.remove(0);
        }
        //System.out.println("=====filterd args = +" + result);
        return result.toArray(new String[0]);
    }

    String[] get() {
        return args;
    }

    private static String[] interpolatedCommandLine(String[] original, JkProperties props) {

        List<String> effectiveCommandLineArgs = new LinkedList<>(Arrays.asList(original));

        // Add arguments contained in local.properties 'jeka.cmd._appendXXXX'
        List<String> appendedArgs = props.getAllStartingWith(CMD_APPEND_PROP, true).keySet().stream()
                .sorted()
                .map(props::get)
                .flatMap(value -> Arrays.stream(JkUtilsString.parseCommandline(value)))
                .collect(Collectors.toList());
        effectiveCommandLineArgs.addAll(appendedArgs);

        // Interpolate arguments passed as $key to respective value
        for (ListIterator<String> it = effectiveCommandLineArgs.listIterator(); it.hasNext(); ) {
            String word = it.next();
            if (word.startsWith(CMD_SUBSTITUTE_SYMBOL)) {
                String token = word.substring(CMD_SUBSTITUTE_SYMBOL.length());
                String propName = CMD_PREFIX_PROP + token;
                String presetValue = props.get(propName);
                if (presetValue != null) {
                    String[] replacingItems = JkUtilsString.parseCommandline(presetValue);
                    it.remove();
                    Arrays.stream(replacingItems).forEach(item -> it.add(item));
                }
            }
        }
        JkLog.verbose("Effective command line : " + effectiveCommandLineArgs);

        return effectiveCommandLineArgs.toArray(new String[0]);
    }
}
