package org.jerkar;

import org.jerkar.tool.JkInit;

class RunPublishDocsOnGithubPages {

    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class, args).publishDocsOnGithubPage();
    }
}
