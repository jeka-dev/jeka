package org.jerkar;

import org.jerkar.tool.JkInit;

class RunPublishLocal {

    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class, "-java#publish.signArtifacts=false, -java#publish.localOnly")
                .javaPlugin.getProject().getMaker().getTasksForPublishing().publish();
    }
}
