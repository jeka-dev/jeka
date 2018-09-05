package org.jerkar.api.java.junit;

import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkClasspath;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.nio.file.Paths;

public class JkUnitRunner {

    public static void main(String[] args) {
        JkJavaTestBulk testSpec = JkJavaTestBulk.of(JkClasspath.current(), JkPathTree.of(Paths.get("./idea-output/test-classes")).accept("**/*MyTest.class"));
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
