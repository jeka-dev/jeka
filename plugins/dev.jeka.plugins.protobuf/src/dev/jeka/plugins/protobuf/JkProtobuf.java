package dev.jeka.plugins.protobuf;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.utils.JkUtilsString;

/**
 * Configurator for projects involving protobuf code generation.
 */
public final class JkProtobuf {

    private String protoPath = "src/main/proto";

    private String protocVersion = JkProtoc.PROTOC_JAR_VERSION;

    private String extraProtocOptions = "";

    private String protobufVersion = "3.21.12";

    private JkProtobuf() {
    }

    public static JkProtobuf of() {
        return new JkProtobuf();
    }

    /**
     * The path (relative to project root) of directory containing the proto files.
     */
    public JkProtobuf setProtoPath(String protoPath) {
        this.protoPath = protoPath;
        return this;
    }

    /**
     * Version of protoc compiler. Should be a version of module 'com.github.os72:protoc-jar'.
     */
    public JkProtobuf setProtocVersion(@JkDepSuggest(versionOnly = true, hint = "com.github.os72:protoc-jar") String protocVersion) {
        this.protocVersion = protocVersion;
        return this;
    }

    /**
     * Extra options to pass to protoc compiler.
     * See <a href="https://manpages.ubuntu.com/manpages/xenial/man1/protoc.1.html">ref</a>
     */
    public JkProtobuf setExtraProtocOptions(String extraProtocOptions) {
        this.extraProtocOptions = extraProtocOptions;
        return this;
    }

    /**
     * The version of com.google.protobuf:protobuf-java to include in compile-time dependencies. <p>
     *
     * If empty or null, this dependencies won't be included.
     * The version will be resolved against coordinate 'com.google.protobuf:protobuf-java'.
     */
    public JkProtobuf setProtobufVersion(
            @JkDepSuggest(versionOnly = true, hint = "com.google.protobuf:protobuf-java:") String protobufVersion) {
        this.protobufVersion = protobufVersion;
        return this;
    }

    /**
     * Configures the specified project by appending a protobuf source generator.
     */
    public void configure(JkProject project) {
        JkProtocSourceGenerator sourceGenerator = JkProtocSourceGenerator.of(project.dependencyResolver.getRepos(),
                protoPath)
                .setExtraProtocOptions(extraProtocOptions)
                .setProtocJarVersion(protocVersion);
        project.compilation.addSourceGenerator(sourceGenerator);
        if (!JkUtilsString.isBlank(protobufVersion)) {
            project.compilation.customizeDependencies(deps -> deps
                    .and("com.google.protobuf:protobuf-java:" + protobufVersion));
        }
    }

}
