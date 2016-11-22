package org.jerkar.plugins.protobuf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.system.JkProcess;

public class JkProtobufProcess {

    /**
     * Initializes a <code>JkProtobufProcess</code> using the 'protoc' command.
     * Assumes 'protoc' is available on the current PATH.
     */
    public static JkProtobufProcess of() {
        return new JkProtobufProcess("protoc");
    }
    
    /**
     * Initializes a <code>JkProtobufProcess</code> using the given the protoc executable file.
     */
    public static JkProtobufProcess of(File protocFile) {
        return new JkProtobufProcess(protocFile.getAbsolutePath());
    }

    private final String protocCommand;
    private final File workingDir;
    private final JkFileTreeSet protoDirs;
    private final JkFileTreeSet protoFiles;
    private final File javaDir;
    
    private JkProtobufProcess(String protocCommand) {
        this.protocCommand = protocCommand;
        this.workingDir = new File(".");
        this.protoDirs = JkFileTreeSet.empty();
        this.protoFiles = JkFileTreeSet.empty();
        this.javaDir = null;
    }
    
    private JkProtobufProcess(String protocCommand, File workingDir, JkFileTreeSet protoDirs, JkFileTreeSet protoFiles, File javaSourceDir) {
        this.protocCommand = protocCommand;
        this.workingDir = workingDir;
        this.protoDirs = protoDirs;
        this.protoFiles = protoFiles;
        this.javaDir = javaSourceDir;
    }
    
    /**
     * Specify a path to protoc.
     */
    public JkProtobufProcess withProtoc(File protocFile) {
        return new JkProtobufProcess(protocFile.getAbsolutePath(), this.workingDir, this.protoDirs, this.protoFiles, this.javaDir);
    }
    
    /**
     * Set the working directory for the protoc process.
     */
    public JkProtobufProcess withWorkingDir(File workingDir) {
        return new JkProtobufProcess(this.protocCommand, workingDir, this.protoDirs, this.protoFiles, this.javaDir);
    }
    
    /**
     * Add a directory to lookup .proto files. See {@link andProtoDirs(JkFileTreeSet)}.
     */
    public JkProtobufProcess andProtoDir(File dir) {
        return andProtoDirs(dir);
    }
    
    /**
     * Add directories to lookup .proto files. See {@link #andProtoDirs(JkFileTreeSet)}.
     */
    public JkProtobufProcess andProtoDirs(File ... dirs) {
        return new JkProtobufProcess(this.protocCommand, this.workingDir, this.protoDirs.and(dirs), this.protoFiles, this.javaDir);
    }
    
    /**
     * Add a directory to lookup .proto files. See {@link #andProtoDirs(JkFileTreeSet)}.
     */
    public JkProtobufProcess andProtoDirs(JkFileTree ... dirs) {
        return new JkProtobufProcess(this.protocCommand, this.workingDir, this.protoDirs.and(dirs), this.protoFiles, this.javaDir);
    }
    
    /**
     * Add directories to lookup .proto files.
     * These directories are passed to protoc with the --proto_path flag.
     * Only the root directory of each {@link JkFileTree} is used.
     * This does not specify individual .proto files, use {@link #andProtoFiles(JkFileTreeSet)} for that.
     */
    public JkProtobufProcess andProtoDirs(JkFileTreeSet dirs) {
        return new JkProtobufProcess(this.protocCommand, this.workingDir, this.protoDirs.and(dirs), this.protoFiles, this.javaDir);
    }
    
    /**
     * Add a .proto file to compile. See {@link #andProtoFile(JkFileTreeSet)}.
     */
    public JkProtobufProcess andProtoFile(File file) {
        JkFileTree files = JkFileTree.of(file.getParentFile())
            .andFilter(JkPathFilter.include(file.getName()));
        return andProtoFiles(files);
    }
    
    /**
     * Add .proto files to compile. See {@link #andProtoFile(JkFileTreeSet)}.
     */
    public JkProtobufProcess andProtoFiles(JkFileTree files) {
        return andProtoFiles(JkFileTreeSet.of(files));
    }
    
    /**
     * Add .proto files to compile.
     * The parent directories of these files are added using {@link andProtoDirs(JkFileTreeSet)},
     * if not added already.
     */
    public JkProtobufProcess andProtoFiles(JkFileTreeSet files) {
    
        // update the proto dirs with the parent dirs of the files
        JkFileTreeSet protoDirs = this.protoDirs;
        for (File file : files) {
            File protoDir = file.getParentFile();
            if (!hasDir(protoDirs, protoDir)) {
                protoDirs = protoDirs.and(protoDir);
            }
        }
        
        return new JkProtobufProcess(this.protocCommand, this.workingDir, protoDirs, this.protoFiles.and(files), this.javaDir);
    }
    
    private boolean hasDir(JkFileTreeSet dirs, File dir) {
        for(JkFileTree tree : dirs.fileTrees()) {
            if (tree.root().equals(dir)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Sets the output java source directory for protoc.
     */
    public JkProtobufProcess withJavaDir(File javaDir) {
        return new JkProtobufProcess(this.protocCommand, this.workingDir, this.protoDirs, this.protoFiles, javaDir);
    }
    
    /**
     * Sets the output java source directory for protoc.
     * If the {@link JkFileTreeSet} contains multiple directories, the first one is used.
     */
    public JkProtobufProcess withJavaDir(JkFileTreeSet treeSet) {
        if (treeSet.fileTrees().isEmpty()) {
            return this;
        }
        File dir = treeSet.fileTrees().get(0).root();
        if (treeSet.fileTrees().size() > 1) {
            JkLog.warn("java dir tree set has multiple directories, picked first one arbitrarily: " + dir);
        }
        return withJavaDir(dir);
    }
    
    /**
     * launch the protoc process
     */
    public void runSync() {
        JkProcess.of(protocCommand, makeArgs())
            .withWorkingDir(workingDir)
            .failOnError(true)
            .runSync();
    }
    
    private String[] makeArgs() {
    
        List<String> args = new ArrayList<String>();
        
        // add proto paths
        for (JkFileTree fileTree : protoDirs.fileTrees()) {
            args.add("--proto_path=" + fileTree.root().getAbsolutePath());
        }
        
        // add java out
        if (javaDir != null) {
            args.add("--java_out=" + javaDir.getAbsolutePath());
        }
        
        // add proto files
        for (File file : protoFiles) {
            args.add(file.getAbsolutePath());
        }
        
        String[] argsArray = new String[args.size()];
        args.toArray(argsArray);
        return argsArray;
    }
}
