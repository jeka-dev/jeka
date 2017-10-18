package org.jerkar.tool;


class BuildRunner extends JkBuild {

    public static void main(String[] args) {
        JkInit.instanceOf(BuildRunner.class, "-help.xmlFile=c:/usertemp/toto.xml").help();
    }

}
