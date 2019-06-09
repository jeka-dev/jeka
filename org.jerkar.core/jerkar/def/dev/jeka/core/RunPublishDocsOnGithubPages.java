package dev.jeka.core;

import dev.jeka.core.tool.JkInit;

class RunPublishDocsOnGithubPages {

    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class, args).publishDocsOnGithubPage();
    }
}
