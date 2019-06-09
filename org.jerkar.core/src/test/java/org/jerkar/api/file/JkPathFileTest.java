package org.jerkar.api.file;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JkPathFileTest {

    @Test
    public void testCopyRepacingTokens() throws Exception {
        Path from = Paths.get(JkPathFileTest.class.getResource("tokenSample.txt").toURI());
        Path to = Files.createTempFile("test", ".txt");
        Map<String, String> tokens = new HashMap<>();
        tokens.put("${1}", "first");
        tokens.put("${2}", "second");
        tokens.put("${3}", "next ${2}");
        Charset utf8 = Charset.forName("UTF-8");
        JkPathFile.of(from).copyReplacingTokens(to, tokens, utf8);
        List<String> resultLines = Files.readAllLines(to, utf8);
        Assert.assertEquals("This is the first line.", resultLines.get(0));
        Assert.assertEquals("This is the second line.", resultLines.get(1));
        Assert.assertEquals("This is the next second line.", resultLines.get(2));
        System.out.println(to);
    }

}
