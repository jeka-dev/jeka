package org.jerkar.tool;

import java.nio.file.Path;

/**
 * Options for help method.
 */
public final class JkHelpOptions {

    @JkDoc("Output help formatted in XML if true. To be used in conjonction ofMany -silent option to parse the output stream friendly.")
    public boolean xml;

    @JkDoc("Output help in this xml file. If this option is specified, no need to specify help.xml option.")
    public Path xmlFile;

    /**
     * Returns true if the help output must be formatted using XML.
     */
    public boolean xml() {
        return xml;
    }

}