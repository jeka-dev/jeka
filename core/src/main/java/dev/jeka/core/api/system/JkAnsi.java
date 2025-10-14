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

package dev.jeka.core.api.system;

public interface JkAnsi {

    static JkAnsi of() {
        return JkAnsiConsole.of().ansi();
    }

    static String colorize(Color color, String string) {
        return JkAnsi.of().fg(color).a(string).reset().toString();
    }

    static String yellow(String string) {
        return colorize(Color.YELLOW, string);
    }

    static String magenta(String string) {
        return colorize(Color.MAGENTA, string);
    }

    static String green(String string) {
        return colorize(Color.GREEN, string);
    }

    static void eraseAllLine() {
        System.out.print(of().eraseLine(EraseKind.ALL).toString());
    }

    static void moveCursorUp(int count) {
        System.out.print(of().cursorUp(count).toString());
    }

    static void moveCursorLeft(int count) {
        System.out.print(of().cursorLeft(count).toString());
    }
    
    JkAnsi fg(Color color);

    JkAnsi fgBright(Color color);

    JkAnsi a(String text);

    JkAnsi render(String text);

    JkAnsi a(Attribute attribute);

    JkAnsi reset();

    JkAnsi cursorUp(int i);

    JkAnsi cursorLeft(int i);

    JkAnsi eraseLine();

    JkAnsi eraseLine(EraseKind eraseKind);

    enum Color {
        BLACK(0, "BLACK"),
        RED(1, "RED"),
        GREEN(2, "GREEN"),
        YELLOW(3, "YELLOW"),
        BLUE(4, "BLUE"),
        MAGENTA(5, "MAGENTA"),
        CYAN(6, "CYAN"),
        WHITE(7, "WHITE"),
        DEFAULT(9, "DEFAULT");

        private final int value;
        private final String name;

        Color(int index, String name) {
            this.value = index;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public int value() {
            return value;
        }

        public int fg() {
            return value + 30;
        }

        public int bg() {
            return value + 40;
        }

        public int fgBright() {
            return value + 90;
        }

        public int bgBright() {
            return value + 100;
        }
    }

    enum Attribute {
        RESET(0, "RESET"),
        INTENSITY_BOLD(1, "INTENSITY_BOLD"),
        INTENSITY_FAINT(2, "INTENSITY_FAINT"),
        ITALIC(3, "ITALIC_ON"),
        UNDERLINE(4, "UNDERLINE_ON"),
        BLINK_SLOW(5, "BLINK_SLOW"),
        BLINK_FAST(6, "BLINK_FAST"),
        NEGATIVE_ON(7, "NEGATIVE_ON"),
        CONCEAL_ON(8, "CONCEAL_ON"),
        STRIKETHROUGH_ON(9, "STRIKETHROUGH_ON"),
        UNDERLINE_DOUBLE(21, "UNDERLINE_DOUBLE"),
        INTENSITY_BOLD_OFF(22, "INTENSITY_BOLD_OFF"),
        ITALIC_OFF(23, "ITALIC_OFF"),
        UNDERLINE_OFF(24, "UNDERLINE_OFF"),
        BLINK_OFF(25, "BLINK_OFF"),
        NEGATIVE_OFF(27, "NEGATIVE_OFF"),
        CONCEAL_OFF(28, "CONCEAL_OFF"),
        STRIKETHROUGH_OFF(29, "STRIKETHROUGH_OFF");

        private final int value;
        private final String name;

        Attribute(int index, String name) {
            this.value = index;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public int value() {
            return value;
        }
    }

    enum EraseKind {
        FORWARD, BACKWARD, ALL
    }


}
