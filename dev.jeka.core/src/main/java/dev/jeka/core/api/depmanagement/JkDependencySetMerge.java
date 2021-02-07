package dev.jeka.core.api.depmanagement;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
        Iterator<JkDependency> leftIt = left.getDependencies().iterator();
        Iterator<JkDependency> rightIt = right.getDependencies().iterator();
        while (leftIt.hasNext()) {
            JkDependency leftDep = leftIt.next();
            JkDependency matchingRightDep = null;
            while (rightIt.hasNext() && matchingRightDep == null) {
                JkDependency rightDep = rightIt.next();
                if (leftDep.equals(rightDep)) {
                    matchingRightDep = rightDep;
                } else if (!left.getDependencies().contains(rightDep) && !result.contains(rightDep)) {
                    absentFromLeft.add(rightDep);
                    result.add(rightDep);
                }
            }
            if (matchingRightDep == null) {
                absentFromRight.add(leftDep);
            } else if (leftDep instanceof JkModuleDependency) {
                JkModuleDependency leftModDep = (JkModuleDependency) leftDep;
                JkModuleDependency rightModDep = (JkModuleDependency) matchingRightDep;
                leftDep = leftModDep.withTransitivity(
                        JkTransitivity.ofDeepest(leftModDep.getTransitivity(), rightModDep.getTransitivity()));
            }
            result.add(leftDep);
        }
        while (rightIt.hasNext()) {
            JkDependency rightDep = rightIt.next();
            absentFromLeft.add(rightDep);
            result.add(rightDep);
        }
        JkVersionProvider mergedVersionProvider = left.getVersionProvider().and(right.getVersionProvider());
        HashSet<JkDepExclude> mergedExcludes = new HashSet<>(left.getGlobalExclusions());
        mergedExcludes.addAll(right.getGlobalExclusions());
        JkDependencySet mergedDependencySet = JkDependencySet.of(result).withGlobalExclusion(mergedExcludes)
                .withVersionProvider(mergedVersionProvider);
        return new JkDependencySetMerge(absentFromLeft, absentFromRight, mergedDependencySet);
    }
}
