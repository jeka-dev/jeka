package dev.jeka.plugins.protobuf;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * A wrapper over the Java implementation of Protoc compiler.
 */
public class JkProtoc {

    /**
     * Module coordinate that should be included in the project as a compile-time dependency in order
     * to use the generated classes.
     */
    public static final JkModuleId PROTOBUF_MODULE = JkModuleId.of("com.google.protobuf:protobuf-java");

    // The coordinate of the module refers to the Java implementation for the protoc compiler.
    private static final String PROTOC_JAR_MODULE = "com.github.os72:protoc-jar";

    // Default version of the Java implementation for the protoc compiler
    public static final String PROTOC_JAR_VERSION = "3.11.4";

    // options to be passed to the protoc compiler. see https://manpages.ubuntu.com/manpages/xenial/man1/protoc.1.html
    private final List<String> protocOptions = new LinkedList<>();

    // The version of the Java implementation for the protoc compiler
    private String protocJarVersion = PROTOC_JAR_VERSION;

    private boolean logCommand = true;

    private boolean logOutput;

    private JkRepoSet repos = JkRepo.ofMavenCentral().toSet();

    private JkProtoc() {
    }

    public static JkProtoc of() {
        return new JkProtoc();
    }

    public static JkProtoc ofJava(Path outputPath) {
        return of().setJavaOutputDir(outputPath);
    }

    /**
     * Compiles the specified proto files.
     * @param protoFiles The proto files to compile.
     * @param extraOptions Extra options to path to the protoc compiler.
     */
    public void compile(JkPathTreeSet protoFiles, String ... extraOptions) {
        List<String> extraOps = new LinkedList<>(Arrays.asList(extraOptions));
        protoFiles.getRootDirsOrZipFiles().forEach(dir -> extraOps.add("--proto_path=" + dir));
        compile(protoFiles.getRelativeFiles(), extraOps.toArray(new String[0]));
    }

    /**
     * Compiles the specified proto files.
     * @param protoFileDir The repository where lies proto files to compile.
     * @param extraOptions Extra options to path to the protoc compiler.
     */
    public void compile(Path protoFileDir, String ... extraOptions) {
        compile(JkPathTreeSet.ofRoots(protoFileDir), extraOptions);
    }

    /**
     * Compiles the specified proto files.
     * @param protoFiles The proto files to compile.
     * @param extraOptions Extra options to path to the protoc compiler.
     */
    public void compile(List<Path> protoFiles, String ...extraOptions) {
        List<Path> outDirs = outDirs();
        JkUtilsAssert.state(!outDirs.isEmpty(), "No output directory has been specified.");
        outDirs.forEach(dir -> JkUtilsPath.createDirectories(dir));
        Path jar = JkCoordinateFileProxy.of(repos, PROTOC_JAR_MODULE + ":" + protocJarVersion).get();
        JkJavaProcess javaProcess = JkJavaProcess.ofJavaJar(jar, null)
                .addParams(protocOptions)
                .addParams(extraOptions)
                .setLogCommand(logCommand)
                .setLogWithJekaDecorator(logOutput);
        for (Path file : protoFiles) {
            javaProcess.addParams(file.toString());
        }
        javaProcess.exec();
        JkLog.info("Protocol buffer compiled " + protoFiles.size() + " files to " + outDirs + ".");
    }

    /**
     * The location of the generated Java sources.
     */
    public JkProtoc setJavaOutputDir(Path path) {
        return addOptions("--java_out=" + path.normalize());
    }

    /**
     * Add a location of which to search for import. Shortcut for option `--proto_path=PATH`.<p>
     * See : https://manpages.ubuntu.com/manpages/xenial/man1/protoc.1.html
     */
    public JkProtoc addProtoPath(Path path) {
        return addOptions("--proto_path=" + path.normalize());
    }

    /**
     * Options to be passed to the protoc compiler.
     * See https://manpages.ubuntu.com/manpages/xenial/man1/protoc.1.html
     */
    public JkProtoc addOptions(String ...options) {
        Arrays.stream(options).forEach(option -> this.protocOptions.add(option));
        return this;
    }

    /**
     * Sets the repositories to fretch the protoc jar compiler from.
     * Default is maven central
     */
    public JkProtoc setRepos(JkRepoSet repos) {
        this.repos = repos;
        return this;
    }

    /**
     * Sets the version of the Java implementation for the protoc compiler.
     */
    public JkProtoc setProtocJarVersion(@JkDepSuggest(versionOnly = true, hint = "com.google.protobuf:protobuf-java:") String protocJarVersion) {
        this.protocJarVersion = protocJarVersion;
        return this;
    }

    public JkProtoc setLogCommand(boolean logCommand) {
        this.logCommand = logCommand;
        return this;
    }

    public JkProtoc setLogOutput(boolean logOutput) {
        this.logOutput = logOutput;
        return this;
    }

    private Optional<String> getOptionValue(String prefix) {
        return protocOptions.stream()
                .filter(option -> option.startsWith(prefix))
                .map(option -> JkUtilsString.substringAfterFirst(option, prefix))
                .findFirst();
    }

    private List<Path> outDirs() {
        final List<Path> result = new LinkedList<>();
        getOptionValue("--java_out=").ifPresent(value -> result.add(Paths.get(value)));
        getOptionValue("--python_out=").ifPresent(value -> result.add(Paths.get(value)));
        getOptionValue("--cpp_out=").ifPresent(value -> result.add(Paths.get(value)));
        return result;
    }

}
