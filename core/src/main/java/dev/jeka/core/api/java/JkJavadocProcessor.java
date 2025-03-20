/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcResult;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsJdk;
import dev.jeka.core.api.utils.JkUtilsSystem;

import javax.tools.DocumentationTool;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides fluent interface for producing Javadoc.
 *
 * @author Jerome Angibaud
 */
public final class JkJavadocProcessor {

    private final List<String> options = new LinkedList<>();

    //private Boolean displayOutput;


    /**
     * Creates a default {@link JkJavadocProcessor} .
     */
    public static JkJavadocProcessor of() {
        return new JkJavadocProcessor();
    }

    /**
     * Returns the axtra arguments passed to the Javadoc tool.
     */
    public List<String> getOptions() {
        return Collections.unmodifiableList(options);
    }

    /**
     * Adds the specified parameters to Javadoc tool.
     */
    public JkJavadocProcessor addOptions(String ... options) {
        return addOptions(Arrays.asList(options));
    }

    /**
     * @see #addOptions(String...)
     */
    public JkJavadocProcessor addOptions(Iterable<String> options) {
        JkUtilsIterable.addAllWithoutDuplicate(this.options, options);
        return this;
    }
    /**
     * Actually processes and creates the javadoc files.
     */
    public void make(Iterable<Path> classpath, JkPathTreeSet srcDirs, Path outputDir) {
        JkLog.startTask("generate-javadoc");
        if (srcDirs.hasNoExistingRoot() || !srcDirs.containFiles()) {
            JkLog.warn("No source file detected. Skip Javadoc.");
            JkLog.endTask();
            return;
        }
        executeCommandLine(classpath, srcDirs, outputDir);
        JkLog.endTask("Javadoc generated in %d mills.");
    }

    // https://www.programcreek.com/java-api-examples/index.php?api=javax.tools.DocumentationTool
    private void executeTool(Iterable<Path> classpath, JkPathTreeSet srcDirs, Path outputDir) {
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            Files.createDirectories(outputDir);
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, JkUtilsIterable.listOf(outputDir.toFile()));
            Writer writer = new PrintWriter(new OutputStreamWriter(JkLog.getOutPrintStream(), StandardCharsets.UTF_8));
            List<String> options = computeOptions(classpath, srcDirs, outputDir);
            DocumentationTool.DocumentationTask task = tool.getTask(writer, fm, null, null,
                    options, null);
            task.call();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void executeCommandLine(Iterable<Path> classpath, JkPathTreeSet srcDirs, Path outputDir) {
        String exeName = JkUtilsSystem.IS_WINDOWS ? "javadoc.exe" : "javadoc";
        Path javadocExe = JkUtilsJdk.javaHome().resolve("bin/" + exeName);
        if (!Files.exists(javadocExe)) {
            javadocExe = JkUtilsJdk.javaHome().resolve("../bin/" + exeName).normalize();
        }
        boolean verbose = JkLog.isVerbose();
        List<String> packages = computePackages(srcDirs);
        if (packages.isEmpty()) {
            JkLog.warn("No package detected. Skip Javadoc.");
            return;
        }
        JkProcess process = JkProcess.of(javadocExe.toString())
                .addParams(computeOptions(classpath, srcDirs, outputDir))
                .addParams(computePackages(srcDirs))
                .setLogWithJekaDecorator(JkLog.isDebug())
                .setLogCommand(verbose)
                .setFailOnError(false);
        JkProcResult result = process.exec();
        if (!result.hasSucceed()) {
            JkLog.warn("An error occurred when generating Javadoc (status error = " + result.getExitCode() + "). Maybe there is no public class to document." +
                    " Relaunch the process with --verbose option to see details");
        }
    }

    private List<String> computePackages(JkPathTreeSet srcDirs) {
        srcDirs = srcDirs.withMatcher(JkJavaCompilerToolChain.JAVA_SOURCE_MATCHER);
        List<String> result = new LinkedList<>();
        for (Path relFile: srcDirs.getRelativeFiles()) {
            Path packageDir = relFile.getParent();
            if (packageDir != null) {
                String packageName = packageDir.toString().replace(File.separator, ".");
                result.add(packageName);
            }
        }
        return result.stream().distinct().collect(Collectors.toList());
    }

    private List<String> computeOptions(Iterable<Path> classpath, JkPathTreeSet srcDirs, Path outputDir) {
        List<String> options = new LinkedList<>();
        if (!containsLike("-Xdoclint")) {
            options.add("-Xdoclint:none");
        }
        if (!contains("-sourcepath")) {
            options.add("-sourcepath");
            options.add(JkPathSequence.of(srcDirs.getRootDirsOrZipFiles()).toPath());
        }
        if (!contains("-d")) {
            options.add("-d");
            options.add(outputDir.toString());
        }
        if (!contains("-classpath") && classpath != null && classpath.iterator().hasNext()) {
            options.add("-classpath");
            options.add(JkPathSequence.of(classpath).toPath());
        }
        if (JkLog.isVerbose()) {
            options.add("-verbose");
        } else {
            options.add("-quiet");
        }
        options.addAll(this.options);
        return options;
    }

    private boolean containsLike(String hint) {
        for (String option : options) {
            if (option.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(String hint) {
        for (String option : options) {
            if (option.equals(hint)) {
                return true;
            }
        }
        return false;
    }

}
