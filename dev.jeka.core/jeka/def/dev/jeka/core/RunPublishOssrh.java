package dev.jeka.core;

import dev.jeka.core.tool.JkInit;

class RunPublishOssrh {

    /**
     * To publish run this method then go to https://oss.sonatype.org and login.
     * Go to 'Staging repositories' then go down to get the the dev.jeka repository and close it.
     * Wait a bit then click on this repository (marked as closed). Next click on 'release' at the top bar.
     * Wait some hours/days before see it on Maven central !
     */
    public static void main(String[] args) {
        CoreBuild coreBuild = JkInit.instanceOf(CoreBuild.class, args);
        //coreBuild.javaPlugin.clean().pack();
        coreBuild.javaPlugin.publish();
    }
}
