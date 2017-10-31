package org.jerkar.tool.builtins.templates.javabuild;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes.Name;

import org.jerkar.api.java.JkManifest;
import org.jerkar.api.utils.JKUtilsTests;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkManifestTest {

    @Test
    public void testWriteMainClass() throws IOException {
        final Path file = Files.createTempFile("manifest", ".mf");
        final String mainClassName = "org.jerkar.Main";
        final JkManifest manifest = JkManifest.empty().addMainAttribute(Name.MAIN_CLASS,
                mainClassName);
        manifest.writeTo(file);
        final String readMainClass = JkManifest.of(file).manifest().getMainAttributes()
                .get(Name.MAIN_CLASS).toString();
        Assert.assertEquals(mainClassName, readMainClass);
    }

}
