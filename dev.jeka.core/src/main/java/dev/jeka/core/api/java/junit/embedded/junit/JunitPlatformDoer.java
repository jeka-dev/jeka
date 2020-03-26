package dev.jeka.core.api.java.junit.embedded.junit;

import dev.jeka.core.api.function.JkUnaryOperator;
import dev.jeka.core.api.java.junit.*;
import dev.jeka.core.api.utils.JkUtilsIO;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.reporting.legacy.xml.LegacyXmlReportGeneratingListener;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class JunitPlatformDoer implements JkInternalJunitDoer {

    static JunitPlatformDoer of() {
        return new JunitPlatformDoer();
    }

    public JkUnit5TestResult launch(JkUnit5.JkEngineBehavior engineBehavior, Serializable testRequest) {
        //System.out.println("-----------" + JkClassLoader.ofCurrent());

        // creating launcher
        LauncherConfig.Builder launcherBuilder = LauncherConfig.builder();
        JkUnaryOperator<LauncherConfig.Builder> launcherEnhancer = engineBehavior.getLauncherEnhancer();
        launcherBuilder = launcherEnhancer == null ? launcherBuilder : launcherEnhancer.apply(launcherBuilder);
        LauncherConfig launcherConfig = launcherBuilder.build();
        Launcher launcher = LauncherFactory.create(launcherConfig);

        // Creating test plan
        LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request();
        if (testRequest instanceof JkUnit5TestSelection) {
            JkUnit5TestSelection testSet = (JkUnit5TestSelection) testRequest;
                requestBuilder
                    .filters(getFilters(testSet))
                    .selectors(
                            DiscoverySelectors.selectClasspathRoots(testSet.getTestClassRoots().toSet())
                    );
        } else if (testRequest instanceof JkUnaryOperator) {
            JkUnaryOperator<LauncherDiscoveryRequestBuilder> enhancer =
                    (JkUnaryOperator<LauncherDiscoveryRequestBuilder>) testRequest;
            requestBuilder = enhancer.apply(requestBuilder);
        }
        TestPlan testPlan = launcher.discover(requestBuilder.build());

        // Setting forced listeners
        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        List<TestExecutionListener> listeners = new LinkedList<>();
        listeners.add(summaryListener);
        TestExecutionListener progressListener = ProgressListeners.get(engineBehavior.getProgressDisplayer());
        if (engineBehavior.getProgressDisplayer() != null) {
            listeners.add(progressListener);
        }
        if (engineBehavior.getLegacyReportDir() != null) {
            LegacyXmlReportGeneratingListener reportGeneratingListener = new LegacyXmlReportGeneratingListener(
                    engineBehavior.getLegacyReportDir(),
                    new PrintWriter(JkUtilsIO.nopOuputStream()));
            listeners.add(reportGeneratingListener);
        }

        // Execution
        launcher.execute(testPlan, listeners.toArray(new TestExecutionListener[0]));
        TestExecutionSummary summary = summaryListener.getSummary();
        return toTestResult(summary);
    }

    private static Filter[] getFilters(JkUnit5TestSelection testSelection) {
        List<Filter> result = new LinkedList<>();
        if (!testSelection.getIncludePatterns().isEmpty()) {
            result.add(ClassNameFilter.includeClassNamePatterns(toArray(testSelection.getIncludePatterns())));
        }
        if (!testSelection.getExcludePatterns().isEmpty()) {
            result.add(ClassNameFilter.excludeClassNamePatterns(toArray(testSelection.getExcludePatterns())));
        }
        if (!testSelection.getIncludeTags().isEmpty()) {
            result.add(TagFilter.includeTags(toArray(testSelection.getIncludeTags())));
        }
        if (!testSelection.getExcludeTags().isEmpty()) {
            result.add(TagFilter.excludeTags(toArray(testSelection.getExcludeTags())));
        }
        return result.toArray(new Filter[0]);
    }

    private static JkUnit5TestResult toTestResult(TestExecutionSummary summary) {
        JkUnit5TestResult.JkCount containerCount = JkUnit5TestResult.JkCount.of(
                summary.getContainersFoundCount(),
                summary.getContainersStartedCount(),
                summary.getContainersSkippedCount(),
                summary.getContainersAbortedCount(),
                summary.getContainersSucceededCount(),
                summary.getContainersFailedCount());
        JkUnit5TestResult.JkCount testCount = JkUnit5TestResult.JkCount.of(
                summary.getTestsFoundCount(),
                summary.getTestsStartedCount(),
                summary.getTestsSkippedCount(),
                summary.getTestsAbortedCount(),
                summary.getTestsSucceededCount(),
                summary.getTestsFailedCount());
        List<JkUnit5TestResult.JkFailure> failures = summary.getFailures().stream()
                .map(JunitPlatformDoer::toFailure).collect(Collectors.toList());
        return JkUnit5TestResult.of(summary.getTimeStarted(), summary.getTimeFinished(),
                containerCount, testCount, failures);
    }

    private static String[] toArray(Set<String> strings) {
        return new ArrayList<String>(strings).toArray(new String[0]);
    }

    private static JkUnit5TestResult.JkFailure toFailure(TestExecutionSummary.Failure failure) {
        JkUnit5TestResult.JkTestIdentifier.JkType type;
        switch (failure.getTestIdentifier().getType()) {
            case CONTAINER:
                type = JkUnit5TestResult.JkTestIdentifier.JkType.CONTAINER;
                break;
            case CONTAINER_AND_TEST:
                type = JkUnit5TestResult.JkTestIdentifier.JkType.CONTAINER_AND_TEST;
                break;
            default:
                type = JkUnit5TestResult.JkTestIdentifier.JkType.TEST;
                break;
        }
        String testId = failure.getTestIdentifier().getUniqueId();
        String displayName = failure.getTestIdentifier().getDisplayName();
        Set<String> tags = failure.getTestIdentifier().getTags().stream().map(TestTag::toString)
                .collect(Collectors.toSet());
        JkUnit5TestResult.JkTestIdentifier id = JkUnit5TestResult.JkTestIdentifier.of(type, testId, displayName, tags);
        return JkUnit5TestResult.JkFailure.of(id, failure.getException().getMessage(),
                failure.getException().getStackTrace());
    }

}
