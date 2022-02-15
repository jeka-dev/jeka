package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.utils.JkUtilsPath;

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
public final class JkJavaCompileSpec<T> {

    public static final String SOURCE_OPTS = "-source";

    public static final String TARGET_OPTS = "-target";

    public static final String PROCESSOR_OPTS = "-processor";

    public static  final String ENCODING_OPTS = "-encoding";

    private static final String CLASSPATH_OPTS = "-cp";

    private static final String OUTPUT_DIR_OPTS = "-d";

    private List<String> options = new LinkedList<>();

    private JkPathTreeSet sources = JkPathTreeSet.ofEmpty();

    /**
     * Owner for parent chaining
     */
    public final T __;

    private JkJavaCompileSpec(T __) {
        this.__ = __;
    }

    public static JkJavaCompileSpec<Void> of() {
        return ofParent(null);
    }

    public static <T> JkJavaCompileSpec<T> ofParent(T o) {
        return new JkJavaCompileSpec<>(o);
    }

    public JkJavaCompileSpec<T> copy() {
        JkJavaCompileSpec<T> copy = JkJavaCompileSpec.ofParent(this.__);
        copy.sources = this.sources;
        copy.options = new LinkedList(this.options);
        return copy;
    }

    /**
     * Returns the specifications as a list of string directly usable in the {@link JkJavaCompiler} except
     * sourcepath
     */
    public List<String> getOptions() {
        return this.options;
    }

    // ------------ Output dir ------------------------

    public Path getOutputDir() {
        final String path = findValueAfter( OUTPUT_DIR_OPTS);
        return path == null ? null : Paths.get(path);
    }

    public JkJavaCompileSpec<T> setOutputDir(Path outputDir) {
        return addOptions(OUTPUT_DIR_OPTS, outputDir.toString());
    }

    // ------- Java version & encoding ----------------

    public JkJavaVersion getSourceVersion() {
        final String rawResult = getNextValue(SOURCE_OPTS);
        if (rawResult == null) {
            return null;
        }
        return JkJavaVersion.of(rawResult);
    }

    public JkJavaVersion getTargetVersion() {
        final String rawResult = getNextValue(TARGET_OPTS);
        if (rawResult == null) {
            return null;
        }
        return JkJavaVersion.of(rawResult);
    }

    /**
     * Sets the version of source code accepted.
     */
    public JkJavaCompileSpec<T> setSourceVersion(JkJavaVersion version) {
        if (version == null) {
            return this;
        }
        return setOption(SOURCE_OPTS, version.get());
    }

    /**
     * Sets the target Java version for compiled classes. Normally it is the same as the
     * platform compileRunner shipped with, but you can set it explicitly. Also, when set explicitly
     * {@link JkJavaCompiler} can choose to use the appropriate compileRunner to compile to the
     * specified target.
     */
    public JkJavaCompileSpec<T> setTargetVersion(JkJavaVersion version) {
        if (version == null) {
            return this;
        }
        return setOption(TARGET_OPTS, version.get());
    }

    /**
     * Shorthand for #setSourceVersion chained to #setTargetVersion
     */
    public JkJavaCompileSpec<T> setSourceAndTargetVersion(JkJavaVersion version) {
        return this.setSourceVersion(version).setTargetVersion(version);
    }

    public String getEncoding() {
        return getNextValue(ENCODING_OPTS);
    }

    /**
     * Creates a copy of this {@link JkJavaCompileSpec} but using the
     * specified source encoding (e.g. UTF-8). If <code>null</code> is specified,
     * then default plateform encoding will be used.
     */
    public JkJavaCompileSpec<T> setEncoding(String encoding) {
        if (encoding == null) {
            return this;
        }
        return setOption(ENCODING_OPTS, encoding);
    }

    // ----------------- source files -----------------------------------------

    /**
     * Adds specified source files to the set of java sources to compile.
     */
    public JkJavaCompileSpec<T> setSources(Function<JkPathTreeSet, JkPathTreeSet> modifier) {
        return setSources(modifier.apply(this.sources));
    }

    public JkJavaCompileSpec<T> setSources(JkPathTreeSet sources) {
        this.sources = sources.mergeDuplicateRoots();
        return this;
    }

    public JkPathTreeSet getSources() {
        return sources;
    }

    public List<Path> getSourceDirs() {
        return sources.toList().stream()
                .map(tree -> tree.getRoot())
                .map(JkUtilsPath::relativizeFromWorkingDir)
                .collect(Collectors.toList());
    }

    // ------------------ classpath --------------------------------

    /**
     * Set the compiler classpath with the specified files
     */
    public JkJavaCompileSpec<T> setClasspath(Iterable<Path> files) {
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
    public JkJavaCompileSpec<T> addOptions(String... options) {
        return this.addOptions(Arrays.asList(options));
    }

    /**
     * See {@link #addOptions(String...)}
     */
    public JkJavaCompileSpec<T> addOptions(Iterable<String> options) {
        options.forEach(option -> this.options.add(option));
        return this;
    }

    /**
     * Some options of a compileRunner are set in a couple of name/value (version, classpath, .....).
     * So if you want to explicitly set such an option it is desirable to remove current value
     * instead of adding it at the queue of options. This method does this for you.
     */
    public JkJavaCompileSpec<T> setOption(String optionName, String optionValue) {
        addOrReplace(optionName, optionValue);
        return this;
    }

    // --------------------------- Annotation processing -----------------------------------

    /**
     * Sets specified annotation classes instead of using the ones discovered by default Java 6 mechanism.
     */
    public JkJavaCompileSpec<T> setAnnotationProcessors(String... annotationProcessorClassNames) {
        return setOption(PROCESSOR_OPTS, String.join(",", annotationProcessorClassNames));
    }

    /**
     * Disable annotation processing.
     */
    public JkJavaCompileSpec<T> disableAnnotationProcessing() {
        return addOptions("-proc:none");
    }

    /**
     * Only process annotation.
     */
    public JkJavaCompileSpec<T> setAnnotationProcessingOnly() {
        return addOptions("-proc:only");
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
                return it.next();
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
