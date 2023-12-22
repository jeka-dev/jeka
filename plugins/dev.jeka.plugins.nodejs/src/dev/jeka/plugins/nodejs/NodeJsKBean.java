package dev.jeka.plugins.nodejs;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;

import java.nio.file.Path;

@JkDoc("Install and run a specified version of NodeJs/npm")
public class NodeJsKBean extends KBean {

    @JkDoc("The version of NodeJs to use")
    @JkDepSuggest(versionOnly = true, hint = "20.10.0,18.19.0,16.20.2")
    public String version = JkNodeJs.DEFAULT_NODE_VERSION;

    @JkDoc("The command line to execute with nodeJs#npm or nodeJs#npx (without command name.")
    public String cmdLine;

    @JkDoc("The relative path of the nodeJs project.")
    public String clientDir = "client";

    @JkDoc("Execute npm using the command line specified in 'cmdLine' property.")
    public void npm() {
        getJkNodeJs().npm(this.cmdLine);
    }

    @JkDoc("Execute npx using the command line specified in 'cmdLine' property.")
    public void npx() {
        getJkNodeJs().npm(this.cmdLine);
    }

    public JkNodeJs getJkNodeJs() {
        return JkNodeJs.ofVersion(version).setWorkingDir(getWorkingDir());
    }

   public Path getWorkingDir() {
        return getBaseDir().resolve(clientDir);
   }

}
