package dev.jeka.core.tool;

import java.util.Optional;

class BehaviorSettings {

    final Optional<String> kbeanName;

    final boolean cleanWork;

    final boolean cleanOutput;

    final boolean forceMode; // ignore jeka-src compilation and dep resolution failures

    final boolean skipCompile;


    BehaviorSettings(String kbeanName, boolean cleanWork, boolean cleanOutput, boolean forceMode,
                     boolean skipCompile) {
        this.kbeanName = Optional.ofNullable(kbeanName);
        this.cleanWork = cleanWork;
        this.cleanOutput = cleanOutput;
        this.forceMode = forceMode;
        this.skipCompile = skipCompile;
    }

    static BehaviorSettings ofDefault() {
        return new BehaviorSettings(null, false, false, false, false);
    }

}
