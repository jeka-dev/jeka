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
import org.fusesource.jansi.Ansi;

import java.util.Arrays;

class JkAnsiImpl implements JkAnsi {

    private final Ansi ansi;

    public JkAnsiImpl(Ansi ansi) {
        this.ansi = ansi;
    }


    @Override
    public JkAnsi fg(Color color) {
        ansi.fg(color.value());
        return this;
    }

    @Override
    public JkAnsi fgBright(Color color) {
        ansi.fgBright(jansiColor(color));
        return this;
    }

    @Override
    public JkAnsi a(String text) {
        ansi.a(text);
        return this;
    }

    @Override
    public JkAnsi a(Attribute attribute) {
        ansi.a(jansiAttribute(attribute));
        return this;
    }

    @Override
    public JkAnsi reset() {
       ansi.reset();
       return this;
    }

    @Override
    public JkAnsi cursorUp(int i) {
        ansi.cursorUp(i);
        return this;
    }

    @Override
    public JkAnsi eraseLine() {
        ansi.eraseLine();
        return this;
    }

    @Override
    public String toString() {
        return ansi.toString();
    }

    private static Ansi.Attribute jansiAttribute(Attribute attribute) {
        return Arrays.stream(Ansi.Attribute.values())
                .filter(ansiAtt -> ansiAtt.value() == attribute.value())
                .findFirst().orElseThrow(
                        () -> new IllegalStateException("Cannot retrieve ANSI attribute " + attribute.name()));
    }

    private static Ansi.Color jansiColor(Color color) {
        return Arrays.stream(Ansi.Color.values())
                .filter(ansiColor -> ansiColor.value() == color.value())
                .findFirst().orElseThrow(
                        () -> new IllegalStateException("Cannot retrieve ANSI color " + color.name()));
    }

}
