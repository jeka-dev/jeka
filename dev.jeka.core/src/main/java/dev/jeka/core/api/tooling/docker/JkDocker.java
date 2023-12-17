package dev.jeka.core.api.tooling.docker;

import dev.jeka.core.api.system.JkProcess;

import java.util.*;

public class JkDocker {

    public static void run(String command, String ...tokens) {
        JkProcess.ofCmdLine(String.format("docker run " + command, tokens))
                .setLogCommand(true)
                .setInheritIO(true)
                .setFailOnError(false)
                .exec();
    }

    public static class Run {

        private final Map<Integer, Integer> portMapping = new HashMap<>();

        private final List<String> extraOptions = new LinkedList<>();

        public Run addExtraOptions(String ...options) {
            extraOptions.addAll(Arrays.asList(options));
            return this;
        }

        public Run addPortMapping(int hostPort, int containerPort) {
            portMapping.put(hostPort, containerPort);
            return this;
        }

        public void run() {

        }
    }
}
