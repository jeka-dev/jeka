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
import dev.jeka.core.api.depmanagement.resolution.JkInternalDependencyResolver;

/*
 * This class is only used with Refection. Please do not remove.
 */
final class IvyInternalDepResolverFactory {

    private static final String IVYRESOLVER_CLASS_NAME = IvyInternalDependencyResolver.class.getName();

    /*
     * Dependency resolver based on Apache Ivy.
     * This resolver is loaded in a dedicated classloader containing Ivy classes.
     * This method is only invoked by reflection. Please do not remove.
     */
    static JkInternalDependencyResolver of(JkRepoSet repos) {
        return IvyInternalDependencyResolver.of(repos);
    }

}
