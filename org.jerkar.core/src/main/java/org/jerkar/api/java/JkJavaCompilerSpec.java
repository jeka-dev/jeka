package org.jerkar.api.java;

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

import java.util.*;

/**
 * Stands for a compilation settings as source and target version, encoding, annotation processing
 * or any option supported by the compiler.
 */
public final class JkJavaCompilerSpec {

    public static final String SOURCE_OPTS = "-source";

    public static final String TARGET_OPTS = "-target";

    public static final String PROCESSOR_OPTS = "-processor";

    public static final String ENCODING_OPTS = "-encoding";

    /**
     * Returns a specification with specified source and target version.
     */
    public static JkJavaCompilerSpec of(JkJavaVersion version) {
        return new JkJavaCompilerSpec(new LinkedList<String>()).withSourceAndTargetVersion(version);
    }

    /**
     * Returns a specification accoriduing the specified raw string options.
     */
    public static JkJavaCompilerSpec of() {
        return new JkJavaCompilerSpec(new ArrayList<>(0));
    }


    /**
     * Returns a specification accoriduing the specified raw string options.
     */
    public static JkJavaCompilerSpec of(List<String> options) {
        return new JkJavaCompilerSpec(options);
    }

    private final List<String> options;

    private JkJavaCompilerSpec(List<String> options) {
        super();
        this.options = options;
    }

    /**
     * Returns the specifications as a list of string directly usable in the {@link JkJavaCompiler}
     */
    public List<String> asOptions() {
        return Collections.unmodifiableList(this.options);
    }

    public String getNextValue(String optionName) {
        return findValueAfter(this.options, optionName);
    }

    public JkJavaVersion getSourceVersion() {
        String rawResult = getNextValue(SOURCE_OPTS);
        if (rawResult == null) {
            return null;
        }
        return JkJavaVersion.name(rawResult);
    }

    public JkJavaVersion getTargetVersion() {
        String rawResult = getNextValue(TARGET_OPTS);
        if (rawResult == null) {
            return null;
        }
        return JkJavaVersion.name(rawResult);
    }

    public String getEncoding() {
        return getNextValue(ENCODING_OPTS);
    }

    /**
     * Creates a copy of this {@link JkJavaCompilerSpec} but adding the specified
     * options. Options are option you pass in javac command line as
     * -deprecation, -nowarn, ... For example, if you want something equivalent
     * to javac -deprecation -cp path1 path2, you should pass "-deprecation",
     * "-cp", "path1", "path2" parameters (all space separated words must stands
     * for one parameter, in other words : parameters must not contain any
     * space).
     */
    public JkJavaCompilerSpec andOptions(String... options) {
        return andOptions(Arrays.asList(options));
    }

    /**
     * Creates a copy of this {@link JkJavaCompilerSpec} but adding the specified
     * options.
     */
    public JkJavaCompilerSpec andOptions(List<String> options) {
        final List<String> newOptions = new LinkedList<String>(this.options);
        newOptions.addAll(options);
        return new JkJavaCompilerSpec(newOptions);
    }

    /**
     * Creates a copy of this {@link JkJavaCompilerSpec} but adding the specified
     * options under specified condition.
     */
    public JkJavaCompilerSpec andOptionsIf(boolean condition, String... options) {
        if (condition) {
            return andOptions(options);
        }
        return this;
    }

    /**
     * Some options of a compiler are set in a couple of name/value (version, classpath, .....).
     * So if you want to explicitly set such an option it is desirable to remove current value
     * instead of adding it at the queue of options.
     */
    public JkJavaCompilerSpec withOption(String optionName, String optionValue) {
        final List<String> newOptions = addOrReplace(options, optionName, optionValue);
        return new JkJavaCompilerSpec(newOptions);
    }

    /**
     * Creates a copy of this {@link JkJavaCompilerSpec} but with the specified
     * source version. If the specified version is null, this methods returns this object.
     */
    public JkJavaCompilerSpec withSourceVersion(JkJavaVersion version) {
        if (version == null) {
            return this;
        }
        return withOption(SOURCE_OPTS, version.name());
    }

    /**
     * Creates a copy of this {@link JkJavaCompilerSpec} but with the target
     * version. If the specified version is null, this methods returns this object.
     */
    public JkJavaCompilerSpec withTargetVersion(JkJavaVersion version) {
        if (version == null) {
            return this;
        }
        return withOption(TARGET_OPTS, version.name());
    }

    /**
     * Shorthand for #withSourceVersion chained to #withTargetVersion
     */
    public JkJavaCompilerSpec withSourceAndTargetVersion(JkJavaVersion version) {
        return this.withSourceVersion(version).withTargetVersion(version);
    }

    /**
     * Creates a copy of this {@link JkJavaCompilerSpec} but using the
     * specified annotation classes instead of using the ones discovered by
     * default Java 6 mechanism.
     */
    public JkJavaCompilerSpec withAnnotationProcessors(String... annotationProcessorClassNames) {
        return withOption(PROCESSOR_OPTS, JkUtilsString.join(annotationProcessorClassNames, ","));
    }

    /**
     * Creates a copy of this {@link JkJavaCompilerSpec} but using the
     * specified source encoding (e.g. UTF-8). If <code>null</code> is specified,
     * then default plateform encoding will be used.
     */
    public JkJavaCompilerSpec withEncoding(String encoding) {
        if (encoding == null) {
            return this;
        }
        return withOption(ENCODING_OPTS, encoding);
    }

    /**
     * Creates a copy of this {@link JkJavaCompilerSpec} but without annotation
     * processing.
     */
    public JkJavaCompilerSpec withoutAnnotationProcessing() {
        return andOptions("-proc:none");
    }

    /**
     * Creates a copy of this {@link JkJavaCompilerSpec} but only for annotation
     * processing (no compilation).
     */
    public JkJavaCompilerSpec withAnnotationProcessingOnly() {
        return andOptions("-proc:only");
    }

    static String findValueAfter(Iterable<String> options, String optionName) {
        Iterator<String> it = options.iterator();
        while (it.hasNext()) {
            String optionItem = it.next();
            if (optionItem.equals(optionName) && it.hasNext()) {
                return it.next();
            }
        }
        return null;
    }

    static List<String> addOrReplace(Iterable<String> options, String optionName, String value) {
        List<String> result = JkUtilsIterable.concatLists(options);
        int index = result.indexOf(optionName);
        if (index > 0) {
            result.remove(index);
            if (index < result.size()) {
                result.remove(index);
            }
        }
        result.add(optionName);
        result.add(value);
        return result;
    }



}
