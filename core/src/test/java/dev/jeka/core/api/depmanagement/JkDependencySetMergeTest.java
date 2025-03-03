package dev.jeka.core.api.depmanagement;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JkDependencySetMergeTest {

    @Test
    public void of_distinctSets_mergeCorrectly() {
        JkDependencySet left = of("A", "B", "C", "D", "E", "F", "Z");
        JkDependencySet right = of("Z","A", "B", "Y", "V", "E", "X");
        JkDependencySet expectedResult = of("A", "B", "C", "D", "E", "F", "Z", "Y", "V", "X");
        JkDependencySetMerge merge = JkDependencySetMerge.of(left, right);
        assertEquals(of("Y", "V", "X").getEntries(), merge.getAbsentDependenciesFromLeft());
        assertEquals(of("C", "D", "F").getEntries(), merge.getAbsentDependenciesFromRight());
        assertEquals(expectedResult.getEntries(), merge.getResult().getEntries());
    }

    private static JkDependencySet of (String ... depNames) {
        List<JkDependency> dependencies = new LinkedList<>();
        for (String depName : depNames) {
            dependencies.add(JkCoordinateDependency.of(depName + ":" + depName));
        }
        return JkDependencySet.of(dependencies);
    }

}