package dev.jeka.core.api.text.ansi;

public interface JkAnsiConsole {

    static JkAnsiConsole of() {
        return new JkAnsiConsoleImpl();
    }

    void systemInstall();

    void systemUninstall();

    JkAnsi ansi();
}
