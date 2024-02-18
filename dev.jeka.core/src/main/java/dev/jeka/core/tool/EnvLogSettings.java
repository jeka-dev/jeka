package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;

// Log settings
class EnvLogSettings {

    final boolean verbose;

    final boolean debug;

    final boolean stackTrace;

    final boolean runtimeInformation;

    final boolean duration;

    final JkLog.Style style;

    final Boolean animation;

    final boolean banner;

    EnvLogSettings(boolean verbose,
                   boolean debug,
                   boolean stackTrace,
                   boolean runtimeInformation,
                   boolean duration,
                   JkLog.Style style,
                   Boolean animation,
                   boolean banner) {

        this.verbose = verbose;
        this.debug = debug;
        this.stackTrace = stackTrace;
        this.runtimeInformation = runtimeInformation;
        this.duration = duration;
        this.style = style;
        this.animation = animation;
        this.banner = banner;
    }

    static EnvLogSettings ofDefault() {
        return new EnvLogSettings(false, false, false, false, false, JkLog.Style.INDENT, false, false);
    }




}
