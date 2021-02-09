package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.url.URLHandlerRegistry;

class IvyTranslatorToIvy {

    static Ivy toIvy(JkRepoSet repoSet) {
        IvySettings ivySettings = ivySettingsOf(repoSet);
        return ivy(ivySettings);
    }

    private static Ivy ivy(IvySettings ivySettings) {
        final Ivy ivy = new Ivy();
        ivy.getLoggerEngine().popLogger();
        ivy.getLoggerEngine().setDefaultLogger(new IvyMessageLogger());
        ivy.getLoggerEngine().setShowProgress(JkLog.verbosity() == JkLog.Verbosity.VERBOSE);
        ivy.getLoggerEngine().clearProblems();
        IvyContext.getContext().setIvy(ivy);
        ivy.setSettings(ivySettings);
        ivy.bind();
        URLHandlerRegistry.setDefault(new IvyFollowRedirectUrlHandler());
        return ivy;
    }

    /**
     * Creates an <code>IvySettings</code> to the specified repositories.
     */
    private static IvySettings ivySettingsOf(JkRepoSet resolveRepos) {
        final IvySettings ivySettings = new IvySettings();
        IvyTranslations.populateIvySettingsWithRepo(ivySettings, resolveRepos);
        ivySettings.setDefaultCache(JkLocator.getJekaRepositoryCache().toFile());
        return ivySettings;
    }


}
