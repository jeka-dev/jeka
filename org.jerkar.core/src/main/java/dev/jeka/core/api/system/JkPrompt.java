package dev.jeka.core.api.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class JkPrompt {

    private JkPrompt() {
    }

    public static String ask(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            return br.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
