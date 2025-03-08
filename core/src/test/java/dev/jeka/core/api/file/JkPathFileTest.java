package dev.jeka.core.api.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class JkPathFileTest {

    @Test
    void testCopyReplacingTokens() throws Exception {
        Path from = Paths.get(JkPathFileTest.class.getResource("tokenSample.txt").toURI());
        Path to = Files.createTempFile("test", ".txt");
        Map<String, String> tokens = new HashMap<>();
        tokens.put("${1}", "first");
        tokens.put("${2}", "second");
        tokens.put("${3}", "next ${2}");
        Charset utf8 = StandardCharsets.UTF_8;
        JkPathFile.of(from).copyReplacingTokens(to, tokens, utf8);
        List<String> resultLines = Files.readAllLines(to, utf8);
        Assertions.assertEquals("This is the first line.", resultLines.get(0));
        Assertions.assertEquals("This is the second line.", resultLines.get(1));
        Assertions.assertEquals("This is the next second line.", resultLines.get(2));
        System.out.println(to);
    }

}
