package org.jerkar.tool;

@SuppressWarnings("javadoc")
public class JkBuildRunner extends JkBuild {

    public static void main(String[] args) {
        JkInit.instanceOf(JkBuildRunner.class, "-help.xmlFile=c:/usertemp/toto.xml").help();
    }

}
