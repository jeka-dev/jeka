package dev.jeka.core.api.java.junit;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClasspath;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.nio.file.Paths;

public class JkUnitRunner {

    public static void main(String[] args) {
        JkJavaTestClasses testSpec = JkJavaTestClasses.of(JkClasspath.ofCurrentRuntime(),
                JkPathTree.of(Paths.get("./idea-output/test-classes"))
                        .andMatching(true, "**/*MyTest.class"));
        JkTestSuiteResult result = JkUnit.of().withBreakOnFailure(false).run(testSpec);
        result.failures().forEach(testCaseFailure -> System.out.println(testCaseFailure.getExceptionDescription().getMessage()));
    }


    public static class MyRunner extends BlockJUnit4ClassRunner {

        public MyRunner(Class<?> klass) throws InitializationError {
            super(klass);
        }

        @Override
        public void run(RunNotifier notifier) {
            notifier.addListener(new RunListener() {
                @Override
                public void testRunFinished(Result result) throws Exception {
                    System.out.println("------------------------------------- finished");
                }
            });
        }
    }
}
