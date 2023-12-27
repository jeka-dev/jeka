package dev.jeka.plugins.nodejs;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.nio.file.Path;
import java.util.stream.Stream;

@JkDoc("Auto-configure projects with nodeJs client.")
public class NodeJsKBean extends KBean {

    @JkDoc("The version of NodeJs to use")
    @JkDepSuggest(versionOnly = true, hint = "20.10.0,18.19.0,16.20.2")
    public String version = JkNodeJs.DEFAULT_NODE_VERSION;

    @JkDoc("Comma separated, command lines to execute for client build or in conjunction with #exec method. " +
            "This can be similar to something like 'npx ..., npm ...'")
    public String cmdLine;

    @JkDoc("The relative path of the nodeJs project.")
    public String clientDir = "client";

    @JkDoc("The directory path, relative to 'clientDir', containing the client build result.")
    public String clientBuildDir = "build";

    @JkDoc("Execute npm using the command line specified in 'cmdLine' property.")
    public void exec() {
        Stream.of(commandLines()).forEach(getJkNodeJs()::exec);
    }

    @Override
    protected void init() {
        getRuntime().getOptionalKBean(ProjectKBean.class).ifPresent(projectKBean -> {
            JkNodeJs.ofVersion(this.version)
                    .configure(projectKBean.project, clientDir, clientBuildDir, commandLines());
        });
    }

    private JkNodeJs getJkNodeJs() {
        return JkNodeJs.ofVersion(version).setWorkingDir(getWorkingDir());
    }

   private Path getWorkingDir() {
        return getBaseDir().resolve(clientDir);
   }

   private String[] commandLines() {
        return Stream.of(cmdLine.split(","))
                .map(String::trim)
                .toArray(String[]::new);
   }

}
