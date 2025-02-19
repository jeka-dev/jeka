package dev.jeka.core.api.text.ansi;

import org.fusesource.jansi.Ansi;

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
    public JkAnsi a(String text) {
        ansi.a(text);
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
}
