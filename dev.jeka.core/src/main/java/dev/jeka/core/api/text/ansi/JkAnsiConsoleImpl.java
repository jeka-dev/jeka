package dev.jeka.core.api.text.ansi;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

class JkAnsiConsoleImpl implements JkAnsiConsole {


    public void systemInstall() {
        AnsiConsole.systemInstall();
    }

    public void systemUninstall() {
        AnsiConsole.systemUninstall();
    }

    public JkAnsi ansi() {
        return new JkAnsiImpl(Ansi.ansi());
    }






}
