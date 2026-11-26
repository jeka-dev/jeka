package dev.jeka.core.tool;

import dev.jeka.core.tool.CommandLine.Model.CommandSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PicocliMainCommandTest {

    @Test
    void programOption_hasTheNameHandledByShellScripts() {
        CommandSpec spec = CommandSpec.forAnnotatedObject(new PicocliMainCommand());

        // 'jeka' and 'jeka.ps1' only handle these names
        assertNotNull(spec.findOption("-p"));
        assertNotNull(spec.findOption("--program"));
    }

}
