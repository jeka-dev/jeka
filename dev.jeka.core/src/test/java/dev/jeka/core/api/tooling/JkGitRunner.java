package dev.jeka.core.api.tooling;

import dev.jeka.core.api.utils.JkUtilsString;

import java.util.Arrays;
import java.util.List;

public class JkGitRunner {

    public static void main(String[] args) {
        String line = JkGit.of()
                .addParams("show", "-s", "--pretty=%d", "HEAD")
                .execAndReturnOutput().get(0);
        System.out.println(line);
        List<String> allItems = Arrays.asList(line.split(","));
        System.out.println(allItems.get(0));
        String result = JkUtilsString.substringAfterFirst(allItems.get(0), "->").trim();
        System.out.println("*" + result + "*");

        System.out.println("--------");
        System.out.println(JkGit.of().getCommitMessagesSinceLastTag());

    }
}
