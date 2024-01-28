package dev.jeka.core.tool.builtins.tooling.ide;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Files;
import java.nio.file.Path;

class IntelliJProject {

    private final Path rootDir;

    private IntelliJProject(Path rootDir) {
        this.rootDir = rootDir;
    }

    public static IntelliJProject find(Path from) {
        from = from.toAbsolutePath();
        if (isProjectRooDir(from)) {
            JkLog.trace("Intellij Parent found at " + from);
            return new IntelliJProject(from);
        }
        if (from.getParent() == null) {
            throw new IllegalStateException("No Intellij project can be found from "
                    + from.normalize().toAbsolutePath());
        }
        return find(from.getParent());
    }

    public IntelliJProject deleteWorkspaceXml() {
        JkUtilsPath.deleteIfExists(rootDir.resolve(".idea/workspace.xml"));
        return this;
    }


    private static boolean isProjectRooDir(Path candidate) {
        if (Files.exists(candidate.resolve(".idea/workspace.xml"))) {
            return true;
        }
        if (Files.exists(candidate.resolve(".idea/modules.xml"))) {
            return true;
        }
        return Files.exists(candidate.resolve(".idea/vcs.xml"));
    }
}
