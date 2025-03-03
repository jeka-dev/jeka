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

package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class Engines {

    private static List<Engine> all;

    static void registerMaster(Engine master) {
        List<Engine> engines = new ArrayList<>();
        engines.add(master);
        engines.addAll(findSubs(master));
        JkLog.debug("Registering master engines:");
        engines.forEach(engine -> JkLog.debug("  " + engine.baseDir.toString()));
        Engines.all = engines;
    }

    static Engine get(Path path) {
        final Path enginePath = path.toAbsolutePath();
        return Engines.all.stream()
                .filter(engine -> engine.baseDir.equals(enginePath))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No engine found at: " + path + " registered engines are: "
                        + all.stream().map(e -> e.baseDir).collect(Collectors.toList())));
    }

    private static List<Engine> findSubs(Engine engine) {
        List<Engine> subEngines = engine.getClasspathSetupResult().subEngines;
        List<Engine> result = new ArrayList<>(subEngines);
        for (Engine subEngine : subEngines) {
            result.addAll(findSubs(subEngine));
        }
        return result.stream().distinct().collect(Collectors.toList());


    }

}
