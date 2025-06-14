/*
 * Copyright 2014-2025  the original author or authors.
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

package dev.jeka.core.api.system.embedded.jansi;

import dev.jeka.core.api.system.JkAnsi;
import dev.jeka.core.api.system.JkAnsiConsole;
import org.jline.jansi.Ansi;
import org.jline.jansi.AnsiConsole;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;

class JkAnsiConsoleImpl implements JkAnsiConsole {

    static JkAnsiConsoleImpl of() {
        return new JkAnsiConsoleImpl();
    }

    public void systemInstall() {
        Terminal terminal;
        try {
            terminal = TerminalBuilder.builder()
                    .system(true)
                    .dumb(true)
                    .jansi(true)
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        AnsiConsole.setTerminal(terminal);
        AnsiConsole.systemInstall();
    }

    public void systemUninstall() {
        AnsiConsole.systemUninstall();
    }

    public JkAnsi ansi() {
        return new JkAnsiImpl(Ansi.ansi());
    }

}
