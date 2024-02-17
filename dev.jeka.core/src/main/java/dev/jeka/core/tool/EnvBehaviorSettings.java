package dev.jeka.core.tool;

import java.util.Optional;

class EnvBehaviorSettings {

    final Optional<String> kbeanName;

    final boolean cleanWork;

    final boolean cleanOutput;

    final boolean ignoreCompileFailure;

    final Optional<String> commandHelp;

    EnvBehaviorSettings(String kbeanName, boolean cleanWork, boolean cleanOutput, boolean ignoreCompileFailure,
                        String commandHelp) {
        this.kbeanName = Optional.ofNullable(kbeanName);
        this.cleanWork = cleanWork;
        this.cleanOutput = cleanOutput;
        this.ignoreCompileFailure = ignoreCompileFailure;
        this.commandHelp = Optional.ofNullable(commandHelp);
    }

    static EnvBehaviorSettings ofDefault() {
        return new EnvBehaviorSettings(null, false, false, false, null);
    }

}
