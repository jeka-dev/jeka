/*
 * Copyright 2014-2025  the original author or authors.
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

package dev.jeka.core.api.java.tools;


import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsJdk;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class JkJpackage {

    private static final String EXEC_NAME = "jpackage" + (JkUtilsSystem.IS_WINDOWS ? ".exe" : "");

    private static final Path EXEC_PATH = Paths.get(System.getProperty("java.home")).resolve("bin").resolve(EXEC_NAME);

    private final Path execPath;

    private final List<OptionGroup> options =  new ArrayList<>();

    private JkJpackage(Path execPath) {
        this.execPath = execPath;
    }

    public static JkJpackage of() {
        return new JkJpackage(EXEC_PATH);
    }

    public static JkJpackage of(Path execPath) {
        return new JkJpackage(execPath);
    }

    public static JkJpackage ofJavaVersion(String javaVersion, String distribution) {
        Path jdkPath = JkUtilsJdk.getJdk(distribution, javaVersion);
        return new JkJpackage(jdkPath.resolve("bin").resolve(EXEC_NAME));
    }



    private List<String> toOptions() {
        return options.stream()
                .flatMap(optionGroup -> optionGroup.toList().stream())
                .toList();
    }

    public JkJpackage addOptions(String... options) {
        this.options.add(OptionGroup.of(options));
        return this;
    }

    public void run() {
        JkLog.info("Running jpackage %s with options:", execPath);
        options.forEach(option -> JkLog.info(String.join(" ", option.toList())));
        JkProcess.of(execPath)
                .addParams(toOptions())
                .setInheritIO(true)
                .exec();
    }

    /**
     * Returns the values passed with the specified options. Returns an optional with empty array if the
     * option was specified without any value. Returns an empty optional if the option is not present.
     *
     *
     * @param optionName The option name, as "--dest'.
     *
     */
    public Optional<List<String>> findOptionValues(String optionName) {
        return options.stream()
                .filter(optionGroup -> optionName.equals(optionGroup.name))
                .map(OptionGroup::values)
                .findFirst();
    }

    public void printHelp() {
        JkProcess.of(execPath)
                .setLogCommand(JkLog.isVerbose())
                .addParams("--help")
                .setInheritIO(true)
                .exec();
    }


    private record OptionGroup(String name, List<String> values) {

        static OptionGroup of(String... args) {
            if (args.length == 1) {
                return new OptionGroup(args[0], List.of());
            }
            return new OptionGroup(args[0], Arrays.asList(args).subList(1, args.length));
        }

        List<String> toList() {
            List<String> result = new LinkedList<>();
            result.add(name);
            result.addAll(values);
            return result;
        }
    }

    /**
     * For some packaging formats (such as --type=app-image), the output directory must be empty.
     * This method removes the directory so the packaged app can be created safely.
     */
    public void prepareOutputDir() {
        String typeOption = getOptionValue("--type");

        // On macOs the destination folder for the package app is expected to not already exist.
        if ("app-image".equals(typeOption)) {
            Path dest = getOptionValue("--dest") == null ? Paths.get("") : Paths.get(getOptionValue("--dest"));
            String appName = getOptionValue("--name");
            if (appName == null) {
                JkLog.warn("jpackage --name option not specified");
                return;
            }
            Path destAppDir = dest.resolve(appName + ".app");
            if (Files.exists(destAppDir)) {
                JkLog.verbose("Clean %s app directory", destAppDir);
                JkUtilsPath.deleteQuietly(destAppDir, true);
            }
        }
    }

    private String getOptionValue(String optionName) {
        return findOptionValues(optionName)
                .filter(list -> !list.isEmpty())
                .map(List::getFirst)
                .orElse(null);
    }



}
