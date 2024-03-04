package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.system.JkLog;
import org.junit.Ignore;
import org.junit.Test;

public class JkNexusReposTest {

    @Test
    @Ignore
    public void testCloseAndRelease() {
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkRepo repo = JkRepo.ofMavenOssrhDeployRelease("djeang", System.getenv("jiraPwd"), null);
        JkNexusRepos.ofRepo(repo).closeAndRelease();
    }

}