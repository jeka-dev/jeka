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

package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.utils.JkUtilsAssert;

/**
 * Contains parameters likely to impact module resolution behavior.
 */
public final class JkResolutionParameters  {

    /**
     * Strategy for resolving version conflict
     */
    public enum JkConflictResolver {

        /**
         * Default conflict resolver. By default, on Ivy it takes the greatest version, unless
         * a version is expressed explicitly in direct dependency.
         */
        DEFAULT,

        /**
         * Fail resolution if a there is a version conflict into the resolved tree. User has to
         * exclude unwanted version explicitly.
         */
        STRICT,

        /**
         * Select the latest version compatible with version constraints. It acts as 'strict' if
         * no version are not expressed using constraints.
         */
        LATEST_COMPATIBLE,

        /**
         * Select the latest version in the resolved dependency tree.
         */
        LATEST_VERSION;
    }

    private boolean refreshed = true;

    private JkConflictResolver conflictResolver = JkConflictResolver.DEFAULT;

    private boolean failOnDependencyResolutionError = true;

    private JkResolutionParameters() {

    }

    public static JkResolutionParameters of() {
        return new JkResolutionParameters();
    }

    /**
     * Returns the conflict resolver to use.
     */
    public JkConflictResolver getConflictResolver() {
        return conflictResolver;
    }

    /**
     * Set the {@link JkConflictResolver} to use.
     */
    public JkResolutionParameters  setConflictResolver(JkConflictResolver conflictResolver) {
        JkUtilsAssert.argument(conflictResolver != null, "conflictResolver can not be null.");
        this.conflictResolver = conflictResolver;
        return this;
    }

    /**
     * Returns <code>true</code> if during the resolution phase, the dynamic
     * version must be resolved as well or the cache can be reused.
     */
    public boolean isRefreshed() {
        return refreshed;
    }

    /**
     * @see JkResolutionParameters#isRefreshed()
     */
    public JkResolutionParameters  setRefreshed(boolean refreshed) {
        this.refreshed = refreshed;
        return this;
    }


    public boolean isFailOnDependencyResolutionError() {
        return failOnDependencyResolutionError;
    }

    /**
     * If <code>true</code> this object will throw a JkException whenever a dependency resolution occurs. Otherwise,
     * just logs a warning message. <code>false</code> by default.
     */
    public JkResolutionParameters  setFailOnDependencyResolutionError(boolean failOnDependencyResolutionError) {
        this.failOnDependencyResolutionError = failOnDependencyResolutionError;
        return this;
    }

    public JkResolutionParameters copy() {
        return JkResolutionParameters.of()
                .setFailOnDependencyResolutionError(this.failOnDependencyResolutionError)
                .setConflictResolver(this.conflictResolver)
                .setRefreshed(refreshed);
    }
}
