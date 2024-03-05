package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;

// Log settings
class LogSettings {

    final boolean verbose;

    final boolean debug;

    final boolean quiet;

    final boolean stackTrace;

    final boolean runtimeInformation;

    final boolean duration;

    final JkLog.Style style;

    final Boolean animation;

    LogSettings(boolean verbose,
                boolean debug,
                boolean quiet,
                boolean stackTrace,
                boolean runtimeInformation,
                boolean duration,
                JkLog.Style style,
                Boolean animation) {

        this.verbose = verbose;
        this.debug = debug;
        this.quiet = quiet;
        this.stackTrace = stackTrace;
        this.runtimeInformation = runtimeInformation;
        this.duration = duration;
        this.style = style;
        this.animation = animation;
    }

    static LogSettings ofDefault() {
        return new LogSettings(false, false, false, false, false, false, JkLog.Style.INDENT, false);
    }




}
