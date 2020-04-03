package dev.jeka.core.api.java.testing.embedded.junitplatform;

import dev.jeka.core.api.function.JkUnaryOperator;
import dev.jeka.core.api.java.testing.JkInternalJunitDoer;
import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.java.testing.JkTestResult;
import dev.jeka.core.api.java.testing.JkTestSelection;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.reporting.legacy.xml.LegacyXmlReportGeneratingListener;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class JunitPlatformDoer implements JkInternalJunitDoer {

    static JunitPlatformDoer of() {
        return new JunitPlatformDoer();
    }

    public JkTestResult launch(JkTestProcessor.JkEngineBehavior engineBehavior, JkTestSelection testSelection) {

        // creating launcher
        LauncherConfig.Builder launcherBuilder = LauncherConfig.builder();
        JkUnaryOperator<LauncherConfig.Builder> launcherEnhancer = engineBehavior.getLauncherConfigurer();
        launcherBuilder = launcherEnhancer == null ? launcherBuilder : launcherEnhancer.apply(launcherBuilder);
        LauncherConfig launcherConfig = launcherBuilder.build();
        Launcher launcher = LauncherFactory.create(launcherConfig);

        // Creating test plan
        LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request()
            .filters(getFilters(testSelection))
            .selectors(
                    DiscoverySelectors.selectClasspathRoots(testSelection.getTestClassRoots().toSet())
            );
        if (testSelection.getDiscoveryConfigurer() != null) {
            requestBuilder = (LauncherDiscoveryRequestBuilder) testSelection.getDiscoveryConfigurer().apply(requestBuilder);
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
        listeners.add(new RestoreJkLogListener());

        // Execution
        launcher.execute(testPlan, listeners.toArray(new TestExecutionListener[0]));
        TestExecutionSummary summary = summaryListener.getSummary();
        return toTestResult(summary);
    }

    private static Filter[] getFilters(JkTestSelection testSelection) {
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

    private static JkTestResult toTestResult(TestExecutionSummary summary) {
        JkTestResult.JkCount containerCount = JkTestResult.JkCount.of(
                summary.getContainersFoundCount(),
                summary.getContainersStartedCount(),
                summary.getContainersSkippedCount(),
                summary.getContainersAbortedCount(),
                summary.getContainersSucceededCount(),
                summary.getContainersFailedCount());
        JkTestResult.JkCount testCount = JkTestResult.JkCount.of(
                summary.getTestsFoundCount(),
                summary.getTestsStartedCount(),
                summary.getTestsSkippedCount(),
                summary.getTestsAbortedCount(),
                summary.getTestsSucceededCount(),
                summary.getTestsFailedCount());
        List<JkTestResult.JkFailure> failures = summary.getFailures().stream()
                .map(JunitPlatformDoer::toFailure).collect(Collectors.toList());
        return JkTestResult.of(summary.getTimeStarted(), summary.getTimeFinished(),
                containerCount, testCount, failures);
    }

    private static String[] toArray(Set<String> strings) {
        return new ArrayList<>(strings).toArray(new String[0]);
    }

    private static JkTestResult.JkFailure toFailure(TestExecutionSummary.Failure failure) {
        JkTestResult.JkTestIdentifier.JkType type;
        switch (failure.getTestIdentifier().getType()) {
            case CONTAINER:
                type = JkTestResult.JkTestIdentifier.JkType.CONTAINER;
                break;
            case CONTAINER_AND_TEST:
                type = JkTestResult.JkTestIdentifier.JkType.CONTAINER_AND_TEST;
                break;
            default:
                type = JkTestResult.JkTestIdentifier.JkType.TEST;
                break;
        }
        String testId = failure.getTestIdentifier().getUniqueId();
        String displayName = failure.getTestIdentifier().getDisplayName();
        Set<String> tags = failure.getTestIdentifier().getTags().stream().map(TestTag::toString)
                .collect(Collectors.toSet());
        JkTestResult.JkTestIdentifier id = JkTestResult.JkTestIdentifier.of(type, testId, displayName, tags);
        return JkTestResult.JkFailure.of(id, failure.getException().getMessage(),
                failure.getException().getStackTrace());
    }

    private static class RestoreJkLogListener implements TestExecutionListener {

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {
            JkLog.JkState.save();
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            JkLog.JkState.restore();
        }
    }

}
