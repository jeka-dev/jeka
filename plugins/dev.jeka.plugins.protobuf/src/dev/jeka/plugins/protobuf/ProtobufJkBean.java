package dev.jeka.plugins.protobuf;

import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

@JkDoc("Provides configuration and methods to compile protoBuffers files.")
public class ProtobufJkBean extends JkBean {

    @JkDoc("The path (relative to project root) of directory containing the proto files.")
    public String protoPath = "src/main/proto";

    @JkDoc("Version of protoc compiler. Should be a version of module 'com.github.os72:protoc-jar'")
    public String protocVersion = JkProtoc.PROTOC_JAR_VERSION;

    @JkDoc("Extra options to pass to protoc compiler. See https://manpages.ubuntu.com/manpages/xenial/man1/protoc.1.html")
    public String extraProtocOptions = "";

    @JkDoc("The version of com.google.protobuf:protobuf-java to include in compile-time dependencies. If empty or null," +
            " this dependencies won't be included automatically.")
    public String protobufVersion = "3.21.12";

    ProtobufJkBean() {
        getBean(ProjectJkBean.class).configure(this::configure);
    }

    private void configure(JkProject project) {
        JkProtocSourceGenerator sourceGenerator = JkProtocSourceGenerator.of(project.dependencyResolver.getRepos(),
                protoPath)
                .setExtraProtocOptions(extraProtocOptions)
                .setProtocJarVersion(protocVersion);
        project.compilation.addSourceGenerator(sourceGenerator);
        if (!JkUtilsString.isBlank(protobufVersion)) {
            project.compilation.configureDependencies(deps -> deps
                    .and("com.google.protobuf:protobuf-java:" + protobufVersion));
        }
    }


}