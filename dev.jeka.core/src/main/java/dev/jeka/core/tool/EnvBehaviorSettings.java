package dev.jeka.core.tool;

import java.util.Optional;

class EnvBehaviorSettings {

    final Optional<String> kbeanName;

    final boolean cleanWork;

    final boolean cleanOutput;

    final boolean ignoreCompileFailure;


    EnvBehaviorSettings(String kbeanName, boolean cleanWork, boolean cleanOutput, boolean ignoreCompileFailure) {
        this.kbeanName = Optional.ofNullable(kbeanName);
        this.cleanWork = cleanWork;
        this.cleanOutput = cleanOutput;
        this.ignoreCompileFailure = ignoreCompileFailure;
    }

    static EnvBehaviorSettings ofDefault() {
        return new EnvBehaviorSettings(null, false, false, false);
    }

}
