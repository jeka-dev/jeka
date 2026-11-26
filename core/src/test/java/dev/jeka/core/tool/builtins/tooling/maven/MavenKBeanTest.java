package dev.jeka.core.tool.builtins.tooling.maven;

import dev.jeka.core.tool.JkException;
import dev.jeka.core.tool.KBeanTestLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenKBeanTest {

    @TempDir
    Path tempDir;

    @Test
    void getPublication_parentBomWithChildWithoutModuleId_explicitError() throws Exception {
        Path dir = tempDir.toRealPath();
        write(dir.resolve("jeka.properties"), "_jeka.child-bases=a\n@maven=on\n@maven.pub.version=1.0\n"
                + "_@maven.pub.parentBom=true\n_@maven.pub.moduleId=org.example:bom\n");
        write(dir.resolve("a/jeka.properties"), "@project=on\n@maven.pub.moduleId=org.example:a\n");
        MavenKBean mavenKBean = KBeanTestLoader.load(dir, MavenKBean.class);

        JkException e = assertThrows(JkException.class, mavenKBean::getPublication);
        assertTrue(e.getMessage().contains("@project.moduleId"), e.getMessage());
    }

    private static void write(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

}
