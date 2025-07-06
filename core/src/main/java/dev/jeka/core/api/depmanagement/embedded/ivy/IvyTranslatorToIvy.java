/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkResolutionParameters;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.conflict.AbstractConflictManager;
import org.apache.ivy.plugins.resolver.AbstractResolver;

class IvyTranslatorToIvy {

    private static final String MAIN_RESOLVER_NAME = "MAIN";

    static Ivy toIvy(JkRepoSet repoSet, JkResolutionParameters parameters) {
        IvySettings ivySettings = ivySettingsOf(repoSet, parameters);
        Ivy ivy = ivy(ivySettings);
        return ivy;
    }

    private static Ivy ivy(IvySettings ivySettings) {
        final Ivy ivy = new Ivy();
        ivy.getLoggerEngine().popLogger();
        ivy.getLoggerEngine().setDefaultLogger(new IvyMessageLogger());
        ivy.getLoggerEngine().setShowProgress(JkLog.isVerbose());
        ivy.getLoggerEngine().clearProblems();
        IvyContext.getContext().setIvy(ivy);
        ivy.setSettings(ivySettings);
        ivy.bind();
        return ivy;
    }

    /**
     * Creates an <code>IvySettings</code> to the specified repositories.
     */
    private static IvySettings ivySettingsOf(JkRepoSet repos, JkResolutionParameters parameters) {
        final IvySettings ivySettings = new IvySettings();
        final AbstractResolver resolver = IvyTranslatorToResolver.toChainResolver(repos);
        resolver.setName(MAIN_RESOLVER_NAME);
        ivySettings.addResolver(resolver);
        ivySettings.setDefaultResolver(MAIN_RESOLVER_NAME);
        AbstractConflictManager conflictManager = IvyTranslatorToConflictManager.toConflictManager(
                parameters.getConflictResolver());
        if (conflictManager != null) {
            conflictManager.setSettings(ivySettings);
            ivySettings.setDefaultConflictManager(conflictManager);
        }
        ivySettings.setDefaultCache(JkLocator.getJekaRepositoryCache().toFile());
        return ivySettings;
    }

}
