package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.system.JkLog;

public class JkNexusReposRunner {

    public static void main(String[] args) {
        JkLog.setDecorator(JkLog.Style.BRACE);
        JkRepo repo = JkRepo.ofMavenOssrhDeployRelease("djeang", System.getenv("jiraPwd"), null);
        JkNexusRepos.ofUrlAndCredentials(repo).closeAndRelease();
    }

}