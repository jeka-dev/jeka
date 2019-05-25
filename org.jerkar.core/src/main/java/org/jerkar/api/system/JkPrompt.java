package org.jerkar.api.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class JkPrompt {

    private JkPrompt() {
    }

    public static String ask(String question) {
        System.out.print(question);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            return br.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
