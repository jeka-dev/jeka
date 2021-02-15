package dev.jeka.core.api.depmanagement.publication;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A  Ivy configuration to configurations mapping declared along each dependency. </p>
 * The left part describe for which purpose you need this dependency (compile, runtime, test). <br/>
 * The right part describe which transitive dependencies you want to retrieve along the dependency. <br/>
 * <p>
 * For example, your component 'A' depends of component 'B' for compiling. 'A' needs jar 'B' itself, but
 * also its transitive dependencies that 'B' declares for 'compile' and 'runtime' purposes. Then, 'A' to 'B'
 * dependency should be declared with configuration mapping 'compile -> compile, runtime';
 *
 * This concept matches strictly with the <i>configuration</i> concept found in Ivy : <a href="http://wrongnotes.blogspot.be/2014/02/simplest-explanation-of-ivy.html">see here.</a>.
 */
public final class JkIvyConfigurationMapping {

    private static final String ARROW = "->";

    /**
     * Useful when using configuration mapping. As documented in Ivy, it stands for the main archive.
     */
    public static final String ARCHIVE_MASTER = "archives(master)";

    public static final String COMPILE = "compile";

    public static final String RUNTIME = "runtime";

    public static final String TEST = "test";

    private final Set<String> left;

    private final Set<String> right;

    private JkIvyConfigurationMapping(Set<String> left, Set<String> right) {
        this.left = left;
        this.right = right;
    }

    public static JkIvyConfigurationMapping of(Set<String> left, Set<String> right) {
        return new JkIvyConfigurationMapping(left, right);
    }

    public static JkIvyConfigurationMapping of(String left, String ... rights) {
        return new JkIvyConfigurationMapping(Collections.singleton(left), Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList(rights))));
    }

    public static JkIvyConfigurationMapping of(String ivyExpression) {
        if (ivyExpression == null) {
            return new JkIvyConfigurationMapping(Collections.emptySet(), Collections.emptySet());
        }
        String[] items = ivyExpression.split(ARROW);
        if (items.length > 2) {
            throw new IllegalArgumentException("More than one '->' detected in iivy expression " + ivyExpression);
        }
        final Set<String> right;
        if (items.length == 1) {
            right = Collections.emptySet();
        } else {
            right = ofPart(items[1]);
        }
        return new JkIvyConfigurationMapping(ofPart(items[0]), right);
    }

    public Set<String> getLeft() {
        return left;
    }

    public String getLeftAsIvYExpression() {
        return left.isEmpty() ? "*" : String.join(", ", left);
    }

    public String getRightAsIvYExpression() {
        return right.isEmpty() ? "*" : String.join(", ", right);
    }

    public Set<String> getRight() {
        return right;
    }

    public boolean hasFromEqualsTo(String... froms) {
        return this.left.equals(new HashSet<>(Arrays.asList(froms)));
    }

    public boolean hasFrom(String from) {
        return this.left.contains(from);
    }

    public String toIvyExpression() {
        return getLeftAsIvYExpression() + " " + ARROW +  " " + getRightAsIvYExpression();
    }

    private static Set<String> ofPart(String comaSeparated) {
        return Arrays.stream(comaSeparated.split(",")).map(String::trim).collect(Collectors.toSet());
    }



}