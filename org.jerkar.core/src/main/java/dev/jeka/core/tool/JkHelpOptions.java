package dev.jeka.core.tool;

import java.nio.file.Path;

/**
 * Options for help method.
 */
public final class JkHelpOptions {

    @JkDoc("Output help formatted in XML if true. To be used in conjonction of -silent option to parse the output getOutputStream friendly.")
    public boolean xml;

    @JkDoc("Output help in this xml file. If xml option is false, no need to specify help.xml option.")
    public Path xmlFile;

    /**
     * Returns true if the help output must be formatted using XML.
     */
    public boolean xml() {
        return xml;
    }

}