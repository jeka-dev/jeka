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

import dev.jeka.core.api.file.JkAbstractPathTree;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stands for a compilation settings as source and target version, encoding, annotation processing
 * or any option supported by the compileRunner.
 */
public final class JkJavaCompileSpec {

    public static final String SOURCE_OPTS = "-source";

    public static final String TARGET_OPTS = "-target";

    public static final String RELEASE_OPTS = "-release";

    public static final String PROCESSOR_OPTS = "-processor";

    public static  final String ENCODING_OPTS = "-encoding";

    private static final String CLASSPATH_OPTS = "-cp";

    private static final String OUTPUT_DIR_OPTS = "-d";

    private static final String GENERATED_SOURCE_OUTPUT_DIR_OPTS = "-s";

    private List<String> options = new LinkedList<>();

    private JkPathTreeSet sources = JkPathTreeSet.ofEmpty();

    private JkJavaCompileSpec() {
    }

    public static JkJavaCompileSpec of() {
        return new JkJavaCompileSpec();
    }

    public JkJavaCompileSpec copy() {
        JkJavaCompileSpec copy = JkJavaCompileSpec.of();
        copy.sources = this.sources;
        copy.options = new LinkedList(this.options);
        return copy;
    }

    /**
     * Returns the specifications as a list of string directly usable in the {@link JkJavaCompilerToolChain} except
     * source path
     */
    public List<String> getOptions() {
        return this.options;
    }

    // ------------ Output dir ------------------------

    public Path getOutputDir() {
        final String path = findValueAfter(OUTPUT_DIR_OPTS);
        return path == null ? null : Paths.get(path);
    }

    // Used by annotation-processor
    public Path getGeneratedSourceOutputDir() {
        final String path = findValueAfter(GENERATED_SOURCE_OUTPUT_DIR_OPTS);
        return path == null ? null : Paths.get(path);
    }

    public JkJavaCompileSpec setOutputDir(Path outputDir) {
        return addOptions(OUTPUT_DIR_OPTS, outputDir.toString());
    }

    public JkJavaCompileSpec setGeneratedSourceOutputDir(Path generatedSourceOutputDir) {
        return addOptions(GENERATED_SOURCE_OUTPUT_DIR_OPTS, generatedSourceOutputDir.toString());
    }

    // ------- Java version & encoding ----------------

    public String getSourceVersion() {
        return getNextValue(SOURCE_OPTS);
    }

    public String getReleaseVersion() {
        return getNextValue(RELEASE_OPTS);
    }

    public String getTargetVersion() {
        return getNextValue(TARGET_OPTS);
    }

    /**
     * Returns the minimum version of Java suitable for compiling, according
     * <i>source</i>, <i>target</i> and <i>release</i> options. <p>
     * Returns <code>null</code> if none of these options is present.
     */
    public JkJavaVersion minJavaVersion() {
        String releaseArg = getReleaseVersion();
        if (!JkUtilsString.isBlank(releaseArg)) {
            return JkJavaVersion.of(releaseArg);
        }
        String targetArg = getTargetVersion();
        if (!JkUtilsString.isBlank(targetArg)) {
            return JkJavaVersion.of(targetArg);
        }
        String sourceArg = getSourceVersion();
        if (!JkUtilsString.isBlank(sourceArg)) {
            return JkJavaVersion.of(sourceArg);
        }
        return null;
    }

    /**
     * Sets option <code>-source</code>
     */
    public JkJavaCompileSpec setSourceVersion(String version) {
        return setOption(SOURCE_OPTS, version);
    }

    /**
     * Sets option <code>-target</code>
     */
    public JkJavaCompileSpec setTargetVersion(String version) {
        return setOption(TARGET_OPTS, version);
    }

    /**
     * Sets option <code>-release</code>
     */
    public JkJavaCompileSpec setReleaseVersion(String version) {
        return setOption(RELEASE_OPTS, version);
    }

    public String getEncoding() {
        return getNextValue(ENCODING_OPTS);
    }

    /**
     * Creates a copy of this {@link JkJavaCompileSpec} but using the
     * specified source encoding (e.g. UTF-8). If <code>null</code> is specified,
     * then default plateform encoding will be used.
     */
    public JkJavaCompileSpec setEncoding(String encoding) {
        if (encoding == null) {
            return this;
        }
        return setOption(ENCODING_OPTS, encoding);
    }

    // ----------------- source files -----------------------------------------

    /**
     * Adds specified source files to the set of java sources to compile.
     */
    public JkJavaCompileSpec setSources(Function<JkPathTreeSet, JkPathTreeSet> modifier) {
        return setSources(modifier.apply(this.sources));
    }

    public JkJavaCompileSpec setSources(JkPathTreeSet sources) {
        this.sources = sources.mergeDuplicateRoots();
        return this;
    }

    public JkPathTreeSet getSources() {
        return sources;
    }

    public List<Path> getSourceDirs() {
        return sources.toList().stream()
                .map(JkAbstractPathTree::getRoot)
                .map(JkUtilsPath::relativizeFromWorkingDir)
                .collect(Collectors.toList());
    }

    // ------------------ classpath --------------------------------

    /**
     * Set the compiler classpath with the specified files
     */
    public JkJavaCompileSpec setClasspath(Iterable<Path> files) {

        // Otherwise it fails when cp is empty
        if (!files.iterator().hasNext()) {
            return this;
        }
        final String classpath = JkPathSequence.of(files).toPath();
        return this.setOption(CLASSPATH_OPTS, classpath);
    }

    public JkPathSequence getClasspath() {
        Iterator<String> it = options.iterator();
        String value = null;
        while (it.hasNext() && value == null) {
            String option = it.next();
            if (option.equals(CLASSPATH_OPTS)) {
                if (it.hasNext()) {
                    value = it.next();
                }
            }
        }
        if (value == null) {
            return JkPathSequence.of();
        }
        if (value.startsWith("\"")) {
            value = value.substring(1);
        }
        if (value.endsWith("\"")) {
            value = value.substring(0, value.length()-1);
        }
        return JkPathSequence.of(Arrays.asList(value.split(File.pathSeparator)).stream()
                .map(Paths::get)
                .collect(Collectors.toList()));
    }


    // ------------------ generic options -------------------------

    /**
     * Adds specified options to pass to the compileRunner.
     * Options are option you pass in javac command line as
     * -deprecation, -nowarn, ... For example, if you want something equivalent
     * to <code>javac -deprecation -cp path1 path2</code>, you should pass "-deprecation",
     * "-cp", "path1", "path2" parameters.
     */
    public JkJavaCompileSpec addOptions(String... options) {
        return this.addOptions(Arrays.asList(options));
    }

    /**
     * See {@link #addOptions(String...)}
     */
    public JkJavaCompileSpec addOptions(Iterable<String> options) {
        options.forEach(option -> this.options.add(option));
        return this;
    }

    public JkJavaCompileSpec prependOptions(String... options) {
        for (int i = options.length-1; i >= 0; i--) {
            this.options.add(0, options[i]);
        }
        return this;
    }

    /**
     * Some options of a compileRunner are set in a couple of name/value (version, classpath, .....).
     * So if you want to explicitly set such an option it is desirable to remove current value
     * instead of adding it at the queue of options. This method does this for you.
     */
    public JkJavaCompileSpec setOption(String optionName, String optionValue) {
        addOrReplace(optionName, optionValue);
        return this;
    }

    // --------------------------- Annotation processing -----------------------------------

    /**
     * Sets specified annotation classes instead of using the ones discovered by default Java 6 mechanism.
     */
    public JkJavaCompileSpec setAnnotationProcessors(String... annotationProcessorClassNames) {
        return setOption(PROCESSOR_OPTS, String.join(",", annotationProcessorClassNames));
    }

    // ----- options as key/values

    public String getNextValue(String optionName) {
        return findValueAfter(optionName);
    }

    private String findValueAfter(String optionName) {
        final Iterator<String> it = options.iterator();
        while (it.hasNext()) {
            final String optionItem = it.next();
            if (optionItem.equals(optionName) && it.hasNext()) {
                return it.next().trim();
            }
        }
        return null;
    }

    private void addOrReplace(String optionName, String value) {
        int index = options.indexOf(optionName);
        while ( index >= 0) {
            options.remove(index);
            if (index < options.size()) {
                options.remove(index);
            }
            index = options.indexOf(optionName);

        }
        options.add(optionName);
        options.add(value);
    }


}
