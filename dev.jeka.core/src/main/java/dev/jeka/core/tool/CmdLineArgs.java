/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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

    boolean isEmpty() {
        return args.length == 0;
    }

    boolean isUsageHelpRequested() {
        return args.length == 0 || args[0].equals("--help") || args[0].equals("-h");
    }

    boolean isVersionHelpRequested() {
        return args.length > 0 && (args[0].equals("--version") || args[0].equals("-V"));
    }

    /**
     * Returns the KBean name at the start of this cmdArgs.
     * return "" (empty) if not specified explicitly.
     */
    String findKbeanName() {
        if (args.length == 0 || args[0].isEmpty()
                || args[0].equals(KBEAN_CMD_SUFFIX) || !isKbeanRef(args[0])) {
            return "";
        }
        return args[0].substring(0, args[0].length() - 1);
    }

    /**
     * Returns same args without leading kbean name arg, if any.
     */
    CmdLineArgs trunkKBeanRef() {
        if (args.length == 0) {
            return this;
        }
        if (isKbeanRef(args[0])) {
            return new CmdLineArgs(Arrays.copyOfRange(args, 1, args.length));
        }
        return this;
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

    private static boolean isKbeanRef(String arg) {
        return arg.endsWith(KBEAN_CMD_SUFFIX);
    }

    private static String[] filterShellArgs(String[] originalArgs) {
        if (originalArgs.length == 0) {
            return originalArgs;
        }
        List<String> result = new LinkedList<>(Arrays.asList(originalArgs));
        String[] candidates = new String[] {"-r", "-ru", "-ur", "--remote"};
        for (String candidate : candidates) {
            int index = result.indexOf(candidate);
            if (index != -1) {
                result.remove(index);
                if (index < result.size()) {
                    result.remove(index);
                }
                return result.toArray(new String[0]);
            }
        }
        return originalArgs;
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
        JkLog.verbose("Effective command line : %s", effectiveCommandLineArgs);

        return effectiveCommandLineArgs.toArray(new String[0]);
    }

    String kbeanContextHelp() {
        List<String> argList  = Arrays.asList(args);
        int helpIndex = argList.indexOf("--help");
        if (helpIndex == -1) {
            helpIndex = argList.indexOf("-h");
        }
        if (helpIndex <= 0) {
            return null;
        }
        String kbeanArg = argList.get(helpIndex -1);
        if (!kbeanArg.endsWith(":")) {
            return null;
        }
        return JkUtilsString.substringBeforeFirst(kbeanArg, ":");
    }
}
