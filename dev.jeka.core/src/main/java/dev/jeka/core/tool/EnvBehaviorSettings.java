package dev.jeka.core.tool;

class EnvBehaviorSettings {

    final String kbeanName;

    final boolean cleanWork;

    final boolean cleanOutput;

    final boolean ignoreCompileFailure;


    EnvBehaviorSettings(String kbeanName, boolean cleanWork, boolean cleanOutput, boolean ignoreCompileFailure) {
        this.kbeanName = kbeanName;
        this.cleanWork = cleanWork;
        this.cleanOutput = cleanOutput;
        this.ignoreCompileFailure = ignoreCompileFailure;
    }

}
