package dev.jeka.plugins.protobuf;

import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkSourceGenerator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Supplier;

public class JkProtocSourceGenerator extends JkSourceGenerator {

    private final String protoFilePath;

    private String extraArgs = "";

    private Supplier<JkRepoSet> repoSupplier;

    private String protocVersion;

    private JkProtocSourceGenerator(JkProject project, String protoFilePath) {
        this.protoFilePath = protoFilePath;
        this.repoSupplier = () -> project.dependencyResolver.getRepos();
    }

    public static JkProtocSourceGenerator of(JkProject project, String protoFilePath) {
        return new JkProtocSourceGenerator(project, protoFilePath);
    }

    public static JkProtocSourceGenerator of(JkProject project) {
        return new JkProtocSourceGenerator(project, "src/main/proto");
    }

    public JkProtocSourceGenerator setExtraArgs(String extraArgs) {
        this.extraArgs = extraArgs;
        return this;
    }

    public JkProtocSourceGenerator setRepos(JkRepoSet repos) {
        this.repoSupplier = () -> repos;
        return this;
    }

    /**
     * The effective version of the compiler to be chosen from https://mvnrepository.com/artifact/com.google.protobuf/protoc
     */
    public JkProtocSourceGenerator setProtocVersion(String protocVersion) {
        this.protocVersion = protocVersion;
        return this;
    }

    @Override
    public String getDirName() {
        return "protobuf";
    }

    @Override
    public void generate(JkProject project, Path sourceDir) {
        JkLog.startTask("Compiling protocol buffer files from " + protoFilePath);
        JkPathTree protoFiles = JkPathTree.of(project.getBaseDir()).goTo(protoFilePath);
        String[] extraArguments = JkUtilsString.translateCommandline(extraArgs);
        JkProtobuf.compile(protoFiles, Arrays.asList(extraArguments), sourceDir, repoSupplier.get(),
                protocVersion);
        JkLog.endTask();
    }
}
