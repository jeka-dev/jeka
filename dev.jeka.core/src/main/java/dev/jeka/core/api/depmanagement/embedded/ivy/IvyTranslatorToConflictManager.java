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

import dev.jeka.core.api.depmanagement.resolution.JkResolutionParameters;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.conflict.AbstractConflictManager;
import org.apache.ivy.plugins.conflict.LatestCompatibleConflictManager;
import org.apache.ivy.plugins.conflict.LatestConflictManager;
import org.apache.ivy.plugins.conflict.StrictConflictManager;
import org.apache.ivy.plugins.latest.LatestRevisionStrategy;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;

class IvyTranslatorToConflictManager {

    static AbstractConflictManager toConflictManager(
                                    JkResolutionParameters.JkConflictResolver conflictResolver) {
        final AbstractConflictManager conflictManager;
        if (conflictResolver == JkResolutionParameters.JkConflictResolver.STRICT) {
            conflictManager = new StrictConflictManager();
        } else if (conflictResolver == JkResolutionParameters.JkConflictResolver.LATEST_COMPATIBLE) {
            conflictManager = new LatestCompatibleConflictManager("LatestCompatible",
                    new LatestRevisionStrategy());
        } else if (conflictResolver == JkResolutionParameters.JkConflictResolver.LATEST_VERSION) {
            conflictManager = new LatestConflictManager("Latest",
                    new LatestRevisionStrategy());
        } else {
            return null;
        }
        return conflictManager;
    }

    static void bind(DefaultModuleDescriptor moduleDescriptor,
                                        AbstractConflictManager conflictManager, IvySettings ivySettings) {
        PatternMatcher patternMatcher = ExactPatternMatcher.INSTANCE;
        conflictManager.setSettings(ivySettings);
        moduleDescriptor.addConflictManager(ModuleId.newInstance("*", "*"), patternMatcher, conflictManager);
    }
}
