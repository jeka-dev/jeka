package dev.jeka.core.tool.builtins.tooling.git;

import dev.jeka.core.tool.KBeanTestLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitKBeanTest {

    @Test
    void lastTag_noTag_saysSo(@TempDir Path tempDir) throws Exception {
        Path dir = tempDir.toRealPath();
        for (String args : new String[] {"init -q", "config user.email a@b.c", "config user.name a",
                "config commit.gpgsign false"}) {
            git(dir, args);
        }
        Files.writeString(dir.resolve("file.txt"), "a");
        git(dir, "add .");
        git(dir, "commit -q -m first");
        GitKBean gitKBean = KBeanTestLoader.load(dir, GitKBean.class);

        PrintStream originalOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true));
        try {
            gitKBean.lastTag();
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(out.toString().contains("No tag found in current branch."), out.toString());
    }

    private static void git(Path dir, String args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args.split(" ")));
        assertEquals(0, new ProcessBuilder(command).directory(dir.toFile()).inheritIO().start().waitFor());
    }

}
