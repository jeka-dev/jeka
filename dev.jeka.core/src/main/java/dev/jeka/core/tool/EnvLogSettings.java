package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;

// Log settings
class EnvLogSettings {

    final boolean verbose;

    final boolean ivyVerbose;

    final boolean startUp;

    final boolean stackTrace;

    final boolean runtimeInformation;

    final boolean totalDuration;

    final JkLog.Style style;

    final Boolean animation;

    final boolean banner;

    EnvLogSettings(boolean verbose, boolean ivyVerbose, boolean startUp,
                          boolean stackTrace, boolean runtimeInformation, boolean totalDuration,
                          JkLog.Style style, Boolean animation, boolean banner) {
        this.verbose = verbose;
        this.ivyVerbose = ivyVerbose;
        this.startUp = startUp;
        this.stackTrace = stackTrace;
        this.runtimeInformation = runtimeInformation;
        this.totalDuration = totalDuration;
        this.style = style;
        this.animation = animation;
        this.banner = banner;
    }

    static EnvLogSettings ofDefault() {
        return new EnvLogSettings(false, false, false, false, false, false, JkLog.Style.INDENT, false, false);
    }




}
