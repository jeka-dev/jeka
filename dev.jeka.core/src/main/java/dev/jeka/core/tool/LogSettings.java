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

package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;

// Log settings
class LogSettings {

    // Initialized for tests
    static LogSettings INSTANCE = ofDefault();

    final boolean verbose;

    final boolean debug;

    final boolean quiet;

    final boolean stackTrace;

    final boolean inspect;

    final boolean duration;

    final JkLog.Style style;

    final Boolean animation;

    final boolean logOnStderr;

    final boolean logIvyVerbose;

    LogSettings(boolean verbose,
                boolean debug,
                boolean quiet,
                boolean stackTrace,
                boolean inspect,
                boolean duration,
                JkLog.Style style,
                Boolean animation,
                boolean logOnStderr,
                boolean logIvyVerbose) {

        this.verbose = verbose;
        this.debug = debug;
        this.quiet = quiet;
        this.stackTrace = stackTrace;
        this.inspect = inspect;
        this.duration = duration;
        this.style = style;
        this.animation = animation;
        this.logOnStderr = logOnStderr;
        this.logIvyVerbose = logIvyVerbose;
    }

    static LogSettings ofDefault() {
        return new LogSettings(false, false, false, false, false, false, JkLog.Style.INDENT, false, false, false);
    }




}
