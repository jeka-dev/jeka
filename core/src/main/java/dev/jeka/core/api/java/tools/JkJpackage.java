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
import java.util.List;
import java.util.function.Supplier;

public final class JkJpackage {

    private static final String EXEC_NAME = "jpackage" + (JkUtilsSystem.IS_WINDOWS ? ".exe" : "");

    private static final Path EXEC_PATH = Paths.get(System.getProperty("java.home")).resolve("bin").resolve(EXEC_NAME);

    private final List<Supplier<List<String>>> options =  new ArrayList<>();

    private JkJpackage() {
    }

    public static JkJpackage of() {
        return new JkJpackage();
    }

    private List<String> toOptions() {
        return options.stream()
                .flatMap(supplier -> supplier.get().stream())
                .toList();
    }

    public JkJpackage addOptions(String... options) {
        this.options.add(new ArrayOpts(options));
        return this;
    }

    public void run() {
        JkLog.info("Running jpackage with options:");
        options.forEach(option -> JkLog.info(String.join(" ", option.get())));
        JkProcess.of(EXEC_PATH)
                .addParams(toOptions())
                .setInheritIO(true)
                .exec();
    }

    public void printHelp() {
        JkProcess.of(EXEC_PATH)
                .addParams("--help")
                .setInheritIO(true)
                .exec();
    }


    private record ArrayOpts(String[] opts) implements Supplier<List<String>> {

        @Override
        public List<String> get() {
            return Arrays.asList(opts);
        }
    }
}
