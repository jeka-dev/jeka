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

package e2e;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.docker.JkDocker;
import dev.jeka.core.api.utils.JkUtilsAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@Disabled
class InstallJekaTest {

    private static final String IMAGE_NAME = "jeka-install-ubuntu";

    private static final Path DOCKER_DIR = dockerDir();

    private static final boolean NO_CACHE = false;

    private static final String COW_SAY_URL = "https://github.com/jeka-dev/demo-cowsay#0.0.6";

    private final JekaCmdLineExecutor executor = new JekaCmdLineExecutor();

    @BeforeAll
    public static void beforeAll() {
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
    }

    @AfterAll
    public static void afterAll() {
        JkLog.restoreToInitialState();
        JkLog.setVerbosity(JkLog.Verbosity.INFO);
    }

    @Test
    void installJeka_ok() {
        // build an image of an empty ubuntu distribution, and install jeka wia curl
        JkDocker.of().addParams("build")
                .addParamsIf(NO_CACHE, "--no-cache")
                .addParamsAsCmdLine("--build-arg CACHEBUST=%s", Instant.now())
                .addParamsAsCmdLine("--progress=plain -t %s .", IMAGE_NAME)
                .setWorkingDir(DOCKER_DIR)
                .setInheritIO(true)
                .exec();

        JkDocker.of().addParams("run", "-rm", "-t", IMAGE_NAME)
                .setWorkingDir(DOCKER_DIR)
                .exec();
    }

    private static Path dockerDir() {
        String candidate = "core/src/test/java/e2e/docker_context";
        Path dockerDir = Paths.get(candidate);
        if (!Files.isDirectory(dockerDir)) {
            dockerDir = Paths.get("../" + candidate);
        }
        JkUtilsAssert.state(Files.exists(dockerDir), "%s does not exist", dockerDir);
        return dockerDir;
    }

}
