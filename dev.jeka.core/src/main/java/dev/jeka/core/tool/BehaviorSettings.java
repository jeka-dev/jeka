package dev.jeka.core.tool;

import java.util.Optional;

class BehaviorSettings {

    final Optional<String> kbeanName;

    final boolean cleanWork;

    final boolean cleanOutput;

    final boolean ignoreCompileFailure;

    final boolean skipCompile;


    BehaviorSettings(String kbeanName, boolean cleanWork, boolean cleanOutput, boolean ignoreCompileFailure,
                     boolean skipCompile) {
        this.kbeanName = Optional.ofNullable(kbeanName);
        this.cleanWork = cleanWork;
        this.cleanOutput = cleanOutput;
        this.ignoreCompileFailure = ignoreCompileFailure;
        this.skipCompile = skipCompile;
    }

    static BehaviorSettings ofDefault() {
        return new BehaviorSettings(null, false, false, false, false);
    }

}