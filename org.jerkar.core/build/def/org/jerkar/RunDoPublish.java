package org.jerkar;

import org.jerkar.tool.JkInit;

class RunDoPublish {

    /**
     * To publish run this method then go to https://oss.sonatype.org and log with 'djeang' user name.
     * Go to 'Staging repositories' then go down to get the the org.jerkar repository and close it.
     * Wait a bit then clicking on this repository (marked as closed), click on 'release' at the top bar.
     * Wait some hours/days before see it on Maven central !
     */
    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class, "-java#publish.signArtifacts=false, -java#publish.localOnly").maker().publish();
    }
}
