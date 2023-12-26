package dev.jeka.plugins.protobuf;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

@JkDoc("Provides configuration and methods to compile protoBuffers files.")
public class ProtobufKBean extends KBean {

    @JkDoc("The path (relative to project root) of directory containing the proto files.")
    public String protoPath = "src/main/proto";

    @JkDoc("Version of protoc compiler. Should be a version of module 'com.github.os72:protoc-jar'")
    public String protocVersion = JkProtoc.PROTOC_JAR_VERSION;

    @JkDoc("Extra options to pass to protoc compiler. See https://manpages.ubuntu.com/manpages/xenial/man1/protoc.1.html")
    public String extraProtocOptions = "";

    @JkDoc("The version of com.google.protobuf:protobuf-java to include in compile-time dependencies. If empty or null," +
            " this dependencies won't be included automatically. " +
            "The version will be resolved against coordinate 'com.google.protobuf:protobuf-java'")
    @JkDepSuggest(versionOnly = true, hint = "com.google.protobuf:protobuf-java:")
    public String protobufVersion = "3.21.12";

    @Deprecated
    protected void init() {
        JkProject project = load(ProjectKBean.class).project;
        JkProtobuf.of()
                .setProtoPath(this.protoPath)
                .setExtraProtocOptions(this.extraProtocOptions)
                .setProtocVersion(this.protocVersion)
                .setProtobufVersion(this.protobufVersion)
                .configure(project);
    }

}
