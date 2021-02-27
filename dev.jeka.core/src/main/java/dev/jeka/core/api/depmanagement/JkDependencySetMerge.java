package dev.jeka.core.api.depmanagement;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Result of merging two {@link JkDependencySet}. <p>
 * Merging consists in taking the dependencies of 2 {@link JkDependencySet} and create a third one containing
 * dependencies of the two ones, eliminating duplicates and preserving order.
 */
public final class JkDependencySetMerge {

    private final List<JkDependency> absentDependenciesFromLeft;

    private final List<JkDependency> absentDependenciesFromRight;

    private final JkDependencySet result;

    private JkDependencySetMerge(List<JkDependency> absentDependenciesFromLeft,
                                List<JkDependency> absentDependenciesFromRight, JkDependencySet result) {
        this.absentDependenciesFromLeft = absentDependenciesFromLeft;
        this.absentDependenciesFromRight = absentDependenciesFromRight;
        this.result = result;
    }

    public List<JkDependency> getAbsentDependenciesFromLeft() {
        return absentDependenciesFromLeft;
    }

    public List<JkDependency> getAbsentDependenciesFromRight() {
        return absentDependenciesFromRight;
    }

    public JkDependencySet getResult() {
        return result;
    }

    public static JkDependencySetMerge of(JkDependencySet left, JkDependencySet right) {
        List<JkDependency> result = new LinkedList<>();
        List<JkDependency> absentFromLeft = new LinkedList<>();
        List<JkDependency> absentFromRight = new LinkedList<>();
        for (JkDependency leftDep : left.getEntries()) {
            JkDependency matchingRightDep = right.getMatching(leftDep);
            if (matchingRightDep == null) {
                absentFromRight.add(leftDep);
            }
            result.add(leftDep);
        }
        for (JkDependency rightDep : right.getEntries()) {
            JkDependency matchingLeftDep = left.getMatching(rightDep);
            if (matchingLeftDep == null) {
                absentFromLeft.add(rightDep);
                result.add(rightDep);
            }
        }
        JkVersionProvider mergedVersionProvider = left.getVersionProvider().and(right.getVersionProvider());
        HashSet<JkDependencyExclusion> mergedExcludes = new HashSet<>(left.getGlobalExclusions());
        mergedExcludes.addAll(right.getGlobalExclusions());
        JkDependencySet mergedDependencySet = JkDependencySet.of(result).withGlobalExclusion(mergedExcludes)
                .withVersionProvider(mergedVersionProvider);
        return new JkDependencySetMerge(absentFromLeft, absentFromRight, mergedDependencySet);
    }
}
