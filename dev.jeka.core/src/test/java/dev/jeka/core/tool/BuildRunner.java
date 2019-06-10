package dev.jeka.core.tool;


class BuildRunner extends JkCommands {

    public static void main(String[] args) {
        JkInit.instanceOf(BuildRunner.class, "-help.xmlFile=c:/usertemp/toto.xml").help();
    }

}
