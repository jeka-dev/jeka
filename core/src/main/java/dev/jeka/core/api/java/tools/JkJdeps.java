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
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public final class JkJdeps {

    private static final String EXEC_NAME = "jdeps" + (JkUtilsSystem.IS_WINDOWS ? ".exe" : "");

    private static final Path EXEC_PATH = Paths.get(System.getProperty("java.home")).resolve("bin").resolve(EXEC_NAME);

    private final List<String> options =  new LinkedList<>();

    private JkJdeps() {
    }

    public static JkJdeps of() {
        return new JkJdeps();
    }


    public JkJdeps addOptions(String... options) {
        this.options.addAll(Arrays.asList(options));
        return this;
    }

    public void run() {
        options.forEach(JkLog::info);
        JkProcess.of(EXEC_PATH)
                .addParams(options)
                .setInheritIO(true)
                .exec();
    }

    public List<String> getModuleDeps(Path jar) {
        JkLog.info("Running jpackage with options:");
        options.forEach(JkLog::info);
        return Arrays.stream(JkProcess.of(EXEC_PATH)
                .addParams("--print-module-deps", jar.toString())
                .setCollectStdout(true)
                .exec()
                .getStdoutAsString().split(","))
                .toList().stream()
                .map(item -> item.replace("\n", ""))
                .toList();
    }

    public void printHelp() {
        JkProcess.of(EXEC_PATH)
                .addParams("--help")
                .setInheritIO(true)
                .exec();
    }

}
